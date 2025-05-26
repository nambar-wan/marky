package com.groom.marky.domain.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.groom.marky.common.constant.SeoulBounds;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
//@JsonTypeName("rectangle")
public class Rectangle implements LocationRestriction {

	private Coordinate low;
	private Coordinate high;

	@JsonCreator
	public Rectangle(
		Coordinate low,
		Coordinate high
	) {
		this.low = low;
		this.high = high;
	}

	public Rectangle(double west, double south, double east, double north) {
		this(new Coordinate(south, west), new Coordinate(north, east));
	}

	public static Rectangle rectOfSeoul() {
		return new Rectangle(
			SeoulBounds.MIN_LNG, SeoulBounds.MIN_LAT,
			SeoulBounds.MAX_LNG, SeoulBounds.MAX_LAT
		);
	}

	@JsonIgnore
	public double getWest() {
		return low.getLongitude();
	}

	@JsonIgnore
	public double getSouth() {
		return low.getLatitude();
	}

	@JsonIgnore
	public double getEast() {
		return high.getLongitude();
	}

	@JsonIgnore
	public double getNorth() {
		return high.getLatitude();
	}

	// 박스 쪼개기
	public List<Rectangle> generateGrid(int rows, int cols) {
		if (rows <= 0 || cols <= 0) {
			throw new IllegalArgumentException("rows, cols must be > 0");
		}

		List<Rectangle> grid = new ArrayList<>(rows * cols);
		double latStep = (getNorth() - getSouth()) / rows;
		double lngStep = (getEast() - getWest()) / cols;

		for (int i = 0; i < rows; i++) {
			double cellSouth = getSouth() + i * latStep;
			double cellNorth = (i == rows - 1 ? getNorth() : cellSouth + latStep);

			for (int j = 0; j < cols; j++) {
				double cellWest = getWest() + j * lngStep;
				double cellEast = (j == cols - 1 ? getEast() : cellWest + lngStep);
				grid.add(new Rectangle(cellWest, cellSouth, cellEast, cellNorth));
			}
		}
		return grid;
	}

	// 4분할
	public List<Rectangle> splitGrid() {
		double midLat = (getSouth() + getNorth()) / 2;
		double midLng = (getWest() + getEast()) / 2;

		List<Rectangle> split = new ArrayList<>(4);
		// southwest
		split.add(new Rectangle(getWest(), getSouth(), midLng, midLat));
		// southeast
		split.add(new Rectangle(midLng, getSouth(), getEast(), midLat));
		// northwest
		split.add(new Rectangle(getWest(), midLat, midLng, getNorth()));
		// northeast
		split.add(new Rectangle(midLng, midLat, getEast(), getNorth()));

		return split;
	}

	@Override
	public String toString() {
		return getWest() + "," + getSouth() + "," + getEast() + "," + getNorth();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Rectangle that = (Rectangle)o;
		return Objects.equals(low, that.low) && Objects.equals(high, that.high);
	}

	@Override
	public int hashCode() {
		return Objects.hash(low, high);
	}
}
