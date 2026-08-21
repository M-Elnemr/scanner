"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Loader2, Plus, Trash2 } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { createTripAction, updateTripAction, type RoomPriceInput, type TripInput } from "@/lib/admin/trips-actions";
import type { AirportOption, CompanyOption, CurrencyOption } from "@/lib/admin/reference-data";
import type { HotelOption } from "@/lib/admin/hotel-picker";
import { today } from "@/lib/format/date";

const ROOM_TYPES: RoomPriceInput["roomType"][] = ["SINGLE", "DOUBLE", "TRIPLE", "QUAD", "CHILD", "INFANT"];
const ROOM_LABEL_KEY: Record<RoomPriceInput["roomType"], "roomSingle" | "roomDouble" | "roomTriple" | "roomQuad" | "roomChild" | "roomInfant"> = {
  SINGLE: "roomSingle",
  DOUBLE: "roomDouble",
  TRIPLE: "roomTriple",
  QUAD: "roomQuad",
  CHILD: "roomChild",
  INFANT: "roomInfant",
};

interface HotelRow {
  hotelId: string;
}

interface TripDraft {
  tripCode: string;
  title: string;
  departureDate: string;
  returnDate: string;
  outboundDepartureAirportId: string;
  outboundArrivalAirportId: string;
  returnDepartureAirportId: string;
  returnArrivalAirportId: string;
  airline: string;
  daysInMakkah: string;
  daysInMadinah: string;
  visaIncluded: boolean;
  transportationIncluded: boolean;
  mealsIncluded: boolean;
  guideIncluded: boolean;
  zamzamIncluded: boolean;
  fastTrainIncluded: boolean;
  description: string;
  currencyId: string;
  availableSeats: string;
  tier: "VIP" | "PREMIUM" | "ECONOMIC";
  makkahHotelRows: HotelRow[];
  madinahHotelRows: HotelRow[];
  commissionPerTraveler: string;
  prices: Record<RoomPriceInput["roomType"], string>;
}

const EMPTY: TripDraft = {
  tripCode: "",
  title: "",
  departureDate: "",
  returnDate: "",
  outboundDepartureAirportId: "",
  outboundArrivalAirportId: "",
  returnDepartureAirportId: "",
  returnArrivalAirportId: "",
  airline: "",
  daysInMakkah: "",
  daysInMadinah: "",
  visaIncluded: false,
  transportationIncluded: false,
  mealsIncluded: false,
  guideIncluded: false,
  zamzamIncluded: false,
  fastTrainIncluded: false,
  description: "",
  currencyId: "",
  availableSeats: "",
  tier: "ECONOMIC",
  makkahHotelRows: [],
  madinahHotelRows: [],
  commissionPerTraveler: "",
  prices: { SINGLE: "", DOUBLE: "", TRIPLE: "", QUAD: "", CHILD: "", INFANT: "" },
};

export function TripForm({
  mode,
  tripId,
  airports,
  currencies,
  companies,
  makkahHotels,
  madinahHotels,
  initial,
}: {
  mode: "create" | "edit";
  tripId?: string;
  airports: AirportOption[];
  currencies: CurrencyOption[];
  companies: CompanyOption[];
  makkahHotels: HotelOption[];
  madinahHotels: HotelOption[];
  initial: Partial<TripDraft> & { companyId?: string };
}) {
  const router = useRouter();
  const t = useTranslations("admin.trips.form");
  const tCommon = useTranslations("admin.common");
  const [pending, startTransition] = useTransition();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [companyId, setCompanyId] = useState(initial.companyId ?? "");
  const [form, setForm] = useState<TripDraft>(() => {
    if (mode !== "create") {
      // A trip created before per-trip commission existed, or one that never had its override set,
      // falls back to showing the owning company's current rate — the field is required, so it must
      // never render blank.
      const owner = companies.find((c) => c.id === initial.companyId);
      return {
        ...EMPTY,
        ...initial,
        commissionPerTraveler: initial.commissionPerTraveler || (owner ? String(owner.commissionPerTraveler) : ""),
      };
    }
    const cairoId = airports.find((a) => a.iataCode === "CAI")?.id ?? airports.find((a) => a.city === "Cairo")?.id ?? "";
    const egpId = currencies.find((c) => c.code === "EGP")?.id ?? "";
    return {
      ...EMPTY,
      currencyId: egpId,
      outboundDepartureAirportId: cairoId,
      returnArrivalAirportId: cairoId,
      visaIncluded: true,
      transportationIncluded: true,
      guideIncluded: true,
      ...initial,
    };
  });

  function handleCompanyChange(id: string) {
    setCompanyId(id);
    // Company commission is only ever the *starting point* for a new trip — pick it up here, but
    // leave it alone if the admin already typed something for this trip.
    if (mode === "create" && !form.commissionPerTraveler) {
      const owner = companies.find((c) => c.id === id);
      if (owner) {
        setForm((f) => ({ ...f, commissionPerTraveler: String(owner.commissionPerTraveler) }));
      }
    }
  }

  const INCLUSIONS: { key: keyof TripDraft; label: string }[] = [
    { key: "visaIncluded", label: t("inclusionVisa") },
    { key: "transportationIncluded", label: t("inclusionTransportation") },
    { key: "mealsIncluded", label: t("inclusionMeals") },
    { key: "guideIncluded", label: t("inclusionGuide") },
    { key: "zamzamIncluded", label: t("inclusionZamzam") },
    { key: "fastTrainIncluded", label: t("inclusionFastTrain") },
  ];

  function set<K extends keyof TripDraft>(key: K, value: TripDraft[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function submit(e: React.FormEvent) {
    e.preventDefault();

    const hotels: TripInput["hotels"] = [...form.makkahHotelRows, ...form.madinahHotelRows]
      .filter((h) => h.hotelId)
      .map((h) => ({ hotelId: h.hotelId }));

    const prices: RoomPriceInput[] = ROOM_TYPES.filter((t) => form.prices[t]).map((t) => ({
      roomType: t,
      price: Number(form.prices[t]),
    }));

    const trip: TripInput = {
      tripCode: form.tripCode,
      title: form.title,
      departureDate: form.departureDate,
      returnDate: form.returnDate,
      outboundDepartureAirportId: form.outboundDepartureAirportId,
      outboundArrivalAirportId: form.outboundArrivalAirportId,
      returnDepartureAirportId: form.returnDepartureAirportId,
      returnArrivalAirportId: form.returnArrivalAirportId,
      airline: form.airline,
      daysInMakkah: form.daysInMakkah ? Number(form.daysInMakkah) : undefined,
      daysInMadinah: form.daysInMadinah ? Number(form.daysInMadinah) : undefined,
      visaIncluded: form.visaIncluded,
      transportationIncluded: form.transportationIncluded,
      mealsIncluded: form.mealsIncluded,
      guideIncluded: form.guideIncluded,
      zamzamIncluded: form.zamzamIncluded,
      fastTrainIncluded: form.fastTrainIncluded,
      description: form.description || undefined,
      currencyId: form.currencyId,
      availableSeats: form.availableSeats ? Number(form.availableSeats) : undefined,
      hotels,
      prices,
      tier: form.tier,
      commissionPerTraveler: form.commissionPerTraveler ? Number(form.commissionPerTraveler) : undefined,
    };

    startTransition(async () => {
      const result =
        mode === "create" ? await createTripAction(companyId, trip) : await updateTripAction(tripId!, trip);

      if (result.ok) {
        setFieldErrors({});
        toast.success(mode === "create" ? t("createdToast") : t("updatedToast"));
        router.push(mode === "create" && result.data?.id ? `/admin/trips/${result.data.id}` : "/admin/trips");
        router.refresh();
      } else {
        setFieldErrors(result.fieldErrors ?? {});
        toast.error(result.error ?? tCommon("somethingWentWrong"));
      }
    });
  }

  return (
    <form onSubmit={submit} className="space-y-6">
      {mode === "create" && (
        <div className="space-y-1.5">
          <Label>{t("companyLabel")}</Label>
          <Select
            value={companyId}
            onValueChange={(v) => v && handleCompanyChange(v)}
            items={companies.map((c) => ({ value: c.id, label: c.name }))}
          >
            <SelectTrigger className="w-full">
              <SelectValue placeholder={t("companyPlaceholder")} />
            </SelectTrigger>
            <SelectContent>
              {companies.map((c) => (
                <SelectItem key={c.id} value={c.id}>
                  {c.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-base">{t("basicsTitle")}</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="tripCode">{t("tripCodeLabel")}</Label>
            <Input id="tripCode" value={form.tripCode} onChange={(e) => set("tripCode", e.target.value)} required />
            {fieldErrors.tripCode && <p className="text-xs text-destructive">{fieldErrors.tripCode}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="title">{t("titleLabel")}</Label>
            <Input id="title" value={form.title} onChange={(e) => set("title", e.target.value)} required />
            {fieldErrors.title && <p className="text-xs text-destructive">{fieldErrors.title}</p>}
          </div>
          <div className="space-y-1.5">
            <Label>{t("tierLabel")}</Label>
            <Select
              value={form.tier}
              onValueChange={(v) => v && set("tier", v as TripDraft["tier"])}
              items={[
                { value: "ECONOMIC", label: t("tierEconomic") },
                { value: "PREMIUM", label: t("tierPremium") },
                { value: "VIP", label: t("tierVip") },
              ]}
            >
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ECONOMIC">{t("tierEconomic")}</SelectItem>
                <SelectItem value="PREMIUM">{t("tierPremium")}</SelectItem>
                <SelectItem value="VIP">{t("tierVip")}</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label>{t("currencyLabel")}</Label>
            <Select
              value={form.currencyId}
              onValueChange={(v) => v && set("currencyId", v)}
              items={currencies.map((c) => ({ value: c.id, label: `${c.code} (${c.symbol})` }))}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder={t("currencyPlaceholder")} />
              </SelectTrigger>
              <SelectContent>
                {currencies.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.code} ({c.symbol})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="departureDate">{t("departureDateLabel")}</Label>
            <Input
              id="departureDate"
              type="date"
              min={today()}
              value={form.departureDate}
              onChange={(e) => set("departureDate", e.target.value)}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="returnDate">{t("returnDateLabel")}</Label>
            <Input
              id="returnDate"
              type="date"
              min={today()}
              value={form.returnDate}
              onChange={(e) => set("returnDate", e.target.value)}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="availableSeats">{t("availableSeatsLabel")}</Label>
            <Input
              id="availableSeats"
              type="number"
              min={1}
              value={form.availableSeats}
              onChange={(e) => set("availableSeats", e.target.value)}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="daysInMakkah">{t("daysInMakkahLabel")}</Label>
            <Input
              id="daysInMakkah"
              type="number"
              min={0}
              value={form.daysInMakkah}
              onChange={(e) => set("daysInMakkah", e.target.value)}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="daysInMadinah">{t("daysInMadinahLabel")}</Label>
            <Input
              id="daysInMadinah"
              type="number"
              min={0}
              value={form.daysInMadinah}
              onChange={(e) => set("daysInMadinah", e.target.value)}
              required
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="commissionPerTraveler">{t("commissionLabel")}</Label>
            <Input
              id="commissionPerTraveler"
              type="number"
              min={0}
              value={form.commissionPerTraveler}
              onChange={(e) => set("commissionPerTraveler", e.target.value)}
              required
            />
            <p className="text-xs text-muted-foreground">{t("commissionHint")}</p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-base">{t("flightsTitle")}</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="airline">{t("airlineLabel")}</Label>
            <Input id="airline" value={form.airline} onChange={(e) => set("airline", e.target.value)} required />
          </div>
          <AirportField label={t("outboundDeparture")} value={form.outboundDepartureAirportId} onChange={(v) => set("outboundDepartureAirportId", v)} airports={airports} placeholder={t("airportPlaceholder")} />
          <AirportField label={t("outboundArrival")} value={form.outboundArrivalAirportId} onChange={(v) => set("outboundArrivalAirportId", v)} airports={airports} placeholder={t("airportPlaceholder")} />
          <AirportField label={t("returnDeparture")} value={form.returnDepartureAirportId} onChange={(v) => set("returnDepartureAirportId", v)} airports={airports} placeholder={t("airportPlaceholder")} />
          <AirportField label={t("returnArrival")} value={form.returnArrivalAirportId} onChange={(v) => set("returnArrivalAirportId", v)} airports={airports} placeholder={t("airportPlaceholder")} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-base">{t("inclusionsTitle")}</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3 sm:grid-cols-2">
          {INCLUSIONS.map((item) => (
            <label key={item.key} className="flex items-center gap-2 text-sm">
              <Checkbox checked={form[item.key] as boolean} onCheckedChange={(v) => set(item.key, v as never)} />
              {item.label}
            </label>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-base">{t("hotelsTitle")}</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <Label>{t("makkahHotelLabel")}</Label>
            <HotelRowsEditor
              rows={form.makkahHotelRows}
              onChange={(rows) => set("makkahHotelRows", rows)}
              options={makkahHotels}
              addLabel={t("addHotel")}
              noneLabel={t("hotelNone")}
            />
          </div>
          <div className="space-y-2">
            <Label>{t("madinahHotelLabel")}</Label>
            <HotelRowsEditor
              rows={form.madinahHotelRows}
              onChange={(rows) => set("madinahHotelRows", rows)}
              options={madinahHotels}
              addLabel={t("addHotel")}
              noneLabel={t("hotelNone")}
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-base">{t("roomPricesTitle")}</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3 sm:grid-cols-3">
          {ROOM_TYPES.map((type) => (
            <div key={type} className="space-y-1.5">
              <Label htmlFor={`price-${type}`}>{t(ROOM_LABEL_KEY[type])}</Label>
              <Input
                id={`price-${type}`}
                type="number"
                min={0}
                value={form.prices[type]}
                onChange={(e) => setForm((f) => ({ ...f, prices: { ...f.prices, [type]: e.target.value } }))}
              />
            </div>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-base">{t("descriptionTitle")}</CardTitle>
        </CardHeader>
        <CardContent>
          <Textarea rows={4} value={form.description} onChange={(e) => set("description", e.target.value)} required />
        </CardContent>
      </Card>

      <Button
        type="submit"
        disabled={
          pending ||
          (mode === "create" && !companyId) ||
          !form.currencyId ||
          !form.outboundDepartureAirportId ||
          !form.outboundArrivalAirportId ||
          !form.returnDepartureAirportId ||
          !form.returnArrivalAirportId ||
          form.makkahHotelRows.filter((h) => h.hotelId).length === 0 ||
          form.madinahHotelRows.filter((h) => h.hotelId).length === 0
        }
      >
        {pending && <Loader2 className="size-4 animate-spin" />}
        {mode === "create" ? t("createSubmit") : t("saveSubmit")}
      </Button>
    </form>
  );
}

/** Repeatable hotel rows for one city — a trip may offer several alternatives per city. */
function HotelRowsEditor({
  rows,
  onChange,
  options,
  addLabel,
  noneLabel,
}: {
  rows: HotelRow[];
  onChange: (rows: HotelRow[]) => void;
  options: HotelOption[];
  addLabel: string;
  noneLabel: string;
}) {
  function update(index: number, patch: Partial<HotelRow>) {
    onChange(rows.map((r, i) => (i === index ? { ...r, ...patch } : r)));
  }

  function remove(index: number) {
    onChange(rows.filter((_, i) => i !== index));
  }

  function add() {
    onChange([...rows, { hotelId: "" }]);
  }

  return (
    <div className="space-y-2">
      {rows.map((row, index) => {
        // A hotel already picked in another row of the same city can't be picked again.
        const availableOptions = options.filter(
          (h) => h.id === row.hotelId || !rows.some((r, i) => i !== index && r.hotelId === h.id),
        );
        return (
          <div key={index} className="flex items-start gap-2">
            <div className="flex-1 space-y-1.5">
              <Select
                value={row.hotelId}
                onValueChange={(v) => update(index, { hotelId: v ?? "" })}
                items={availableOptions.map((h) => ({ value: h.id, label: h.name }))}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder={noneLabel} />
                </SelectTrigger>
                <SelectContent>
                  {availableOptions.map((h) => (
                    <SelectItem key={h.id} value={h.id}>
                      {h.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <Button type="button" variant="ghost" size="icon" className="text-destructive" onClick={() => remove(index)}>
              <Trash2 className="size-4" />
            </Button>
          </div>
        );
      })}
      <Button type="button" variant="outline" size="sm" onClick={add}>
        <Plus className="size-3.5" /> {addLabel}
      </Button>
    </div>
  );
}

function AirportField({
  label,
  value,
  onChange,
  airports,
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  airports: AirportOption[];
  placeholder?: string;
}) {
  const locale = useLocale() as "ar" | "en";
  const airportLabel = (a: AirportOption) => {
    const city = locale === "ar" && a.cityAr ? a.cityAr : a.city;
    const country = locale === "ar" && a.countryNameAr ? a.countryNameAr : a.countryName;
    return `${city} (${a.iataCode}) — ${country}`;
  };

  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      <Select value={value} onValueChange={(v) => v && onChange(v)} items={airports.map((a) => ({ value: a.id, label: airportLabel(a) }))}>
        <SelectTrigger className="w-full">
          <SelectValue placeholder={placeholder} />
        </SelectTrigger>
        <SelectContent>
          {airports.map((a) => (
            <SelectItem key={a.id} value={a.id}>
              {airportLabel(a)}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
