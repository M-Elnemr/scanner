package com.umrah.scanner.messaging.application;

import com.umrah.scanner.airport.domain.Airport;
import com.umrah.scanner.company.domain.CompanyAddress;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.hotel.domain.Hotel;
import com.umrah.scanner.hotel.domain.HotelCity;
import com.umrah.scanner.trip.domain.RoomPrice;
import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripHotel;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Composes the ready-to-send text behind the admin dashboard's two WhatsApp buttons. Every literal
 * lives in this one class on purpose — this is the first Arabic text anywhere in the backend, and
 * keeping it out of the use cases it draws data from is what stops it leaking there the way the
 * English notification bodies did.
 *
 * <p>Takes fully-resolved {@link Trip} / {@link CompanyProfile} entities rather than a bare
 * {@code Lead} id, deliberately: {@code Lead}'s own entity graph fetch-joins only {@code trip},
 * {@code company} and {@code customer} themselves, not their nested collections and to-one
 * references (hotels, room prices, airports, addresses). The caller is expected to have already
 * loaded those through {@code TripQueryService.ownedDetail} and {@code CompanyQueryService.getById}
 * — the same fully-initialized entities their own response mappers use — rather than this class
 * re-deciding how to fetch them.
 */
@Component
public class WhatsAppMessageComposer {

    private static final DateTimeFormatter DATE_EN = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_AR = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ar"))
            .withDecimalStyle(DecimalStyle.STANDARD);

    /** Everything a customer needs to recognise and act on the trip they preserved. */
    public String composeTripMessage(
            Trip trip, String customerName, int adultCount, int childCount, int infantCount, WhatsAppLanguage lang) {
        boolean ar = lang == WhatsAppLanguage.AR;
        StringBuilder m = new StringBuilder();

        m.append(ar
                ? "السلام عليكم مع حضرتك شركه عمره اسكان حضرتك حجزت معانا رحله\n\n"
                : "Hello, this is Omra Eskan. You've booked a trip with us.\n\n");
        m.append(ar ? "تفاصيل رحلتك: " : "Here are your trip details: ").append(trip.getTitle())
                .append(" (").append(trip.getTripCode()).append(")\n\n");

        DateTimeFormatter date = ar ? DATE_AR : DATE_EN;
        m.append(ar ? "📅 السفر: " : "📅 Departure: ").append(trip.getDepartureDate().format(date)).append('\n');
        m.append(ar ? "📅 العودة: " : "📅 Return: ").append(trip.getReturnDate().format(date)).append('\n');
        m.append(ar ? "✈️ الطيران: " : "✈️ Airline: ").append(trip.getAirline()).append('\n');
        m.append(route(trip, ar)).append('\n');
        m.append(ar ? "🕌 مكة: " : "🕌 Makkah: ").append(trip.getDaysInMakkah()).append(ar ? " أيام" : " nights").append('\n');
        m.append(ar ? "🕌 المدينة: " : "🕌 Madinah: ").append(trip.getDaysInMadinah()).append(ar ? " أيام" : " nights").append("\n\n");

        // EnumMap (not the entity's own @OrderBy("city asc"), which sorts alphabetically and would
        // put Madinah first) so Makkah always lists ahead of Madinah, matching every other city
        // presentation in this message.
        Map<HotelCity, List<TripHotel>> hotelsByCity = new EnumMap<>(HotelCity.class);
        for (TripHotel tripHotel : trip.getHotels()) {
            hotelsByCity.computeIfAbsent(tripHotel.getCity(), c -> new ArrayList<>()).add(tripHotel);
        }
        for (Map.Entry<HotelCity, List<TripHotel>> entry : hotelsByCity.entrySet()) {
            m.append(groupedHotelLine(entry.getKey(), entry.getValue(), ar)).append('\n');
        }
        m.append('\n');

        List<String> inclusions = inclusions(trip, ar);
        if (!inclusions.isEmpty()) {
            m.append(ar ? "✅ يشمل: " : "✅ Included: ").append(String.join(ar ? "، " : ", ", inclusions)).append("\n\n");
        }

        for (RoomPrice price : trip.getRoomPrices()) {
            m.append(roomLine(price, trip, ar)).append('\n');
        }
        m.append('\n');

        m.append(travelersLine(adultCount, childCount, infantCount, ar)).append('\n');
        BigDecimal total = totalCost(trip, adultCount, childCount, infantCount);
        if (total != null) {
            m.append(ar ? "💰 الإجمالي: " : "💰 Total: ")
                    .append(formatMoney(total, trip.getCurrency().getCode(), ar));
        }

        return m.toString();
    }

    private String travelersLine(int adultCount, int childCount, int infantCount, boolean ar) {
        StringBuilder line = new StringBuilder(ar ? "👥 عدد المسافرين: " : "👥 Travelers: ");
        line.append(ar ? adultCount + " بالغ" : adultCount + " adult" + (adultCount == 1 ? "" : "s"));
        if (childCount > 0) {
            line.append(ar ? "، " + childCount + " طفل" : ", " + childCount + " child" + (childCount == 1 ? "" : "ren"));
        }
        if (infantCount > 0) {
            line.append(ar ? "، " + infantCount + " رضيع" : ", " + infantCount + " infant" + (infantCount == 1 ? "" : "s"));
        }
        return line.toString();
    }

    /** Adults price at the QUAD (4-bed) rate, matching how "price starts from" is quoted elsewhere. */
    private BigDecimal totalCost(Trip trip, int adultCount, int childCount, int infantCount) {
        BigDecimal total = BigDecimal.ZERO;
        boolean priced = false;
        for (RoomPrice price : trip.getRoomPrices()) {
            int count = switch (price.getRoomType()) {
                case QUAD -> adultCount;
                case CHILD -> childCount;
                case INFANT -> infantCount;
                default -> 0;
            };
            if (count > 0) {
                total = total.add(price.getPrice().multiply(BigDecimal.valueOf(count)));
                priced = true;
            }
        }
        return priced ? total : null;
    }

    /**
     * Sent to the company, not the customer — everything they need to recognise the booking and
     * reach the traveler: which trip, its dates, and the customer's own contact and party size.
     */
    public String composeCustomerConfirmationMessage(
            Trip trip, String customerName, String customerPhone,
            int adultCount, int childCount, int infantCount, WhatsAppLanguage lang) {
        boolean ar = lang == WhatsAppLanguage.AR;
        StringBuilder m = new StringBuilder();

        m.append(ar
                ? "السلام عليكم، برجاء تأكيد هذا الحجز:\n\n"
                : "Hello, please confirm this booking:\n\n");

        m.append(ar ? "الرحلة: " : "Trip: ").append(trip.getTitle()).append(" (").append(trip.getTripCode()).append(")\n");
        DateTimeFormatter date = ar ? DATE_AR : DATE_EN;
        m.append(ar ? "📅 السفر: " : "📅 Departure: ").append(trip.getDepartureDate().format(date)).append('\n');
        m.append(ar ? "📅 العودة: " : "📅 Return: ").append(trip.getReturnDate().format(date)).append("\n\n");

        m.append(ar ? "بيانات العميل:\n" : "Customer details:\n");
        m.append(ar ? "👤 الاسم: " : "👤 Name: ").append(customerName).append('\n');
        m.append(ar ? "📱 الهاتف: " : "📱 Phone: ").append(customerPhone).append('\n');
        m.append(travelersLine(adultCount, childCount, infantCount, ar));

        return m.toString();
    }

    /** The operator's own identity and how to reach every branch. */
    public String composeCompanyMessage(CompanyProfile company, String customerName, WhatsAppLanguage lang) {
        boolean ar = lang == WhatsAppLanguage.AR;
        StringBuilder m = new StringBuilder();

        m.append(ar ? "بيانات الشركة المنظمة لرحلتك:\n\n" : "Here are the details of the company running your trip:\n\n");

        m.append("🏢 ").append(company.getCompanyName()).append('\n');
        m.append(ar ? "📄 رقم الترخيص: " : "📄 Licence: ").append(company.getLicenseNumber()).append('\n');
        if (company.getRatingCount() > 0) {
            m.append(ar ? "⭐ التقييم: " : "⭐ Rating: ")
                    .append(company.getRatingAvg()).append("/5 (").append(company.getRatingCount())
                    .append(ar ? " تقييم)" : " reviews)").append('\n');
        }
        if (company.getWhatsapp() != null && !company.getWhatsapp().isBlank()) {
            m.append(ar ? "📱 واتساب: " : "📱 WhatsApp: ").append(company.getWhatsapp()).append('\n');
        }
        if (company.getDescription() != null && !company.getDescription().isBlank()) {
            m.append('\n').append(company.getDescription()).append('\n');
        }

        m.append('\n').append(ar ? "الفروع:" : "Branches:").append('\n');
        for (CompanyAddress address : company.getAddresses()) {
            m.append("📍 ").append(ar ? address.getCity().getNameAr() : address.getCity().getName())
                    .append(" — ").append(address.getAddressText())
                    .append(" (").append(address.getMobileNumber()).append(")\n");
        }

        return m.toString();
    }

    private String route(Trip trip, boolean ar) {
        return (ar ? "🛫 المسار: " : "🛫 Route: ")
                + airportLabel(trip.getOutboundDepartureAirport(), ar) + " → " + airportLabel(trip.getOutboundArrivalAirport(), ar)
                + "  |  " + airportLabel(trip.getReturnDepartureAirport(), ar) + " → " + airportLabel(trip.getReturnArrivalAirport(), ar);
    }

    private String airportLabel(Airport airport, boolean ar) {
        return ar ? airport.getCityAr() : airport.getIataCode();
    }

    /**
     * One line per city, joining every hotel offered there with "or" — a trip may list several
     * alternatives per city, and the customer picks one once they're actually booking, not from this
     * message. Distance-to-haram is dropped here (it doesn't merge cleanly across an "or") for every
     * city, including the common one-hotel case, so a city's line never reads inconsistently
     * depending on how many options happen to be listed.
     */
    private String groupedHotelLine(HotelCity city, List<TripHotel> tripHotels, boolean ar) {
        StringBuilder line = new StringBuilder("🏨 ").append(cityLabel(city, ar)).append(": ");
        List<String> names = tripHotels.stream().map(th -> hotelNameWithStars(th.getHotel(), ar)).toList();
        line.append(String.join(ar ? " أو " : " or ", names));
        line.append(shuttleSuffix(tripHotels, ar));
        return line.toString();
    }

    private String hotelNameWithStars(Hotel hotel, boolean ar) {
        return (ar && hotel.getNameAr() != null ? hotel.getNameAr() : hotel.getName())
                + " (" + hotel.getStars() + (ar ? " نجوم" : "★") + ")";
    }

    /** Says once, for the whole city line, whether the free shuttle applies to all, some, or none of the listed hotels. */
    private String shuttleSuffix(List<TripHotel> tripHotels, boolean ar) {
        long withShuttle = tripHotels.stream().filter(th -> th.getHotel().isFreeBusIncluded()).count();
        if (withShuttle == 0) {
            return "";
        }
        if (withShuttle == tripHotels.size()) {
            return ar ? " — باص مجاني" : " — free shuttle";
        }
        return ar ? " — بعض الفنادق بباص مجاني" : " — some hotels include a free shuttle";
    }

    private String cityLabel(HotelCity city, boolean ar) {
        if (!ar) {
            return city == HotelCity.MAKKAH ? "Makkah" : "Madinah";
        }
        return city == HotelCity.MAKKAH ? "مكة" : "المدينة";
    }

    private List<String> inclusions(Trip trip, boolean ar) {
        List<String> items = new java.util.ArrayList<>();
        if (trip.isVisaIncluded()) {
            items.add(ar ? "التأشيرة" : "Visa");
        }
        if (trip.isTransportationIncluded()) {
            items.add(ar ? "المواصلات" : "Transport");
        }
        if (trip.isMealsIncluded()) {
            items.add(ar ? "الوجبات" : "Meals");
        }
        if (trip.isGuideIncluded()) {
            items.add(ar ? "مرشد" : "Guide");
        }
        if (trip.isZamzamIncluded()) {
            items.add(ar ? "ماء زمزم" : "Zamzam water");
        }
        if (trip.isFastTrainIncluded()) {
            items.add(ar ? "قطار الحرمين" : "Haramain train");
        }
        return items;
    }

    private String roomLine(RoomPrice price, Trip trip, boolean ar) {
        return "💵 " + roomTypeLabel(price.getRoomType(), ar) + ": " + formatMoney(price.getPrice(), trip.getCurrency().getCode(), ar);
    }

    private String roomTypeLabel(RoomType type, boolean ar) {
        if (!ar) {
            return switch (type) {
                case SINGLE -> "Single"; case DOUBLE -> "Double"; case TRIPLE -> "Triple";
                case QUAD -> "Quad"; case CHILD -> "Child"; case INFANT -> "Infant";
            };
        }
        return switch (type) {
            case SINGLE -> "مفرد"; case DOUBLE -> "مزدوج"; case TRIPLE -> "ثلاثي";
            case QUAD -> "رباعي"; case CHILD -> "طفل"; case INFANT -> "رضيع";
        };
    }

    private String formatMoney(BigDecimal amount, String currencyCode, boolean ar) {
        String suffix = ar && "EGP".equals(currencyCode) ? "جنيه" : currencyCode;
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " " + suffix;
    }
}
