package com.groom.marky.domain.response;

import java.util.List;

public record GooglePlacesApiResponse(List<Place> places, String nextPageToken) {
	public record Place(String id, List<String> types, String formattedAddress, Location location, double rating,
						int userRatingCount, DisplayName displayName, List<Review> reviews,

						// Optional Boolean Fields
						Boolean allowsDogs, Boolean curbsidePickup, Boolean delivery, Boolean dineIn,
						Boolean goodForChildren, Boolean goodForGroups, Boolean goodForWatchingSports,
						Boolean liveMusic, Boolean menuForChildren, Boolean outdoorSeating, Boolean reservable,
						Boolean restroom, Boolean servesBeer, Boolean servesBreakfast, Boolean servesBrunch,
						Boolean servesCocktails, Boolean servesCoffee, Boolean servesDessert, Boolean servesDinner,
						Boolean servesLunch, Boolean servesVegetarianFood, Boolean servesWine, Boolean takeout,

						// Nested object for paymentOptions
						PaymentOptions paymentOptions
						, ParkingOptions parkingOptions

						){
		public record Location(double latitude, double longitude) {
		}

		public record DisplayName(String text, String languageCode) {
		}

		public record Review(ReviewText text) {
			public record ReviewText(String text, String languageCode) {
			}
		}

		public record PaymentOptions(Boolean acceptsCreditCards, Boolean acceptsDebitCards, Boolean acceptsCashOnly) {
		}

		public record ParkingOptions( // ⬅️ parking 관련 옵션들
									  Boolean freeParkingLot, Boolean paidParkingLot, Boolean freeStreetParking,
									  Boolean paidStreetParking, Boolean valetParking, Boolean freeGarageParking,
									  Boolean paidGarageParking) {
		}
	}
}
