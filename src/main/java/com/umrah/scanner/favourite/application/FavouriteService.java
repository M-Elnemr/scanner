package com.umrah.scanner.favourite.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.customer.infrastructure.CustomerProfileRepository;
import com.umrah.scanner.favourite.domain.Favourite;
import com.umrah.scanner.favourite.infrastructure.FavouriteRepository;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavouriteService {

    private final FavouriteRepository favouriteRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TripRepository tripRepository;

    public FavouriteService(
            FavouriteRepository favouriteRepository,
            CustomerProfileRepository customerProfileRepository,
            TripRepository tripRepository) {
        this.favouriteRepository = favouriteRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional
    public Favourite addFavourite(UUID customerId, UUID tripId) {
        Favourite favourite = favouriteRepository.findByCustomerIdAndTripId(customerId, tripId).orElseGet(() -> {
            if (!customerProfileRepository.existsById(customerId)) {
                throw NotFoundException.of("CustomerProfile", customerId);
            }
            if (!tripRepository.existsById(tripId)) {
                throw NotFoundException.of("Trip", tripId);
            }
            Favourite newFavourite = new Favourite();
            newFavourite.setCustomer(customerProfileRepository.getReferenceById(customerId));
            newFavourite.setTrip(tripRepository.getReferenceById(tripId));
            return favouriteRepository.save(newFavourite);
        });
        // The controller maps TripSummaryResponse after this transaction/session closes
        // (open-in-view is off). Initializing the trip proxy alone is not enough — the response
        // also walks the trip's currency, its airports and each airport's country. Re-read through
        // the graph that already fetches all of them instead of hand-initializing each one.
        favourite.setTrip(tripRepository.findWithDetailsById(tripId)
                .orElseThrow(() -> NotFoundException.of("Trip", tripId)));
        return favourite;
    }

    @Transactional
    public void removeFavourite(UUID customerId, UUID tripId) {
        favouriteRepository.deleteByCustomerIdAndTripId(customerId, tripId);
    }

    @Transactional(readOnly = true)
    public Page<Favourite> listFavourites(UUID customerId, Pageable pageable) {
        return favouriteRepository.findAllByCustomerId(customerId, pageable);
    }
}
