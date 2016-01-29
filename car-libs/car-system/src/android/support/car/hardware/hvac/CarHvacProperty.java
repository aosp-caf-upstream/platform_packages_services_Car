/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.car.hardware.hvac;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.car.annotation.VersionDef;
import android.support.car.os.ExtendableParcelable;

import java.util.Arrays;
import java.util.Objects;

/**
 * CarHvacProperty object corresponds to a property of the car's HVAC system
 * @hide
 */
public class CarHvacProperty extends ExtendableParcelable {
    private static final int VERSION = 1;

    @VersionDef(version = 1)
    private final int   mPropertyId;
    @VersionDef(version = 1)
    private final int   mType;
    @VersionDef(version = 1)
    private int         mZone;
    @VersionDef(version = 1)
    private final float mFloatMax;
    @VersionDef(version = 1)
    private final float mFloatMin;
    @VersionDef(version = 1)
    private float[]     mFloatValues;
    @VersionDef(version = 1)
    private final int   mIntMax;
    @VersionDef(version = 1)
    private final int   mIntMin;
    @VersionDef(version = 1)
    private int[]       mIntValues;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        int startingPosition = writeHeader(out);
        out.writeInt(mPropertyId);
        out.writeInt(mType);
        out.writeInt(mZone);
        out.writeFloat(mFloatMax);
        out.writeFloat(mFloatMin);
        out.writeFloatArray(mFloatValues);
        out.writeInt(mIntMax);
        out.writeInt(mIntMin);
        out.writeIntArray(mIntValues);
        completeWriting(out, startingPosition);
    }

    public static final Parcelable.Creator<CarHvacProperty> CREATOR
            = new Parcelable.Creator<CarHvacProperty>() {
        public CarHvacProperty createFromParcel(Parcel in) {
            return new CarHvacProperty(in);
        }

        public CarHvacProperty[] newArray(int size) {
            return new CarHvacProperty[size];
        }
    };

    private CarHvacProperty(Parcel in) {
        super(in, VERSION);
        int lastPosition = readHeader(in);
        mPropertyId = in.readInt();
        mType = in.readInt();
        mZone = in.readInt();
        mFloatMax = in.readFloat();
        mFloatMin = in.readFloat();
        mFloatValues = in.createFloatArray();
        mIntMax = in.readInt();
        mIntMin = in.readInt();
        mIntValues = in.createIntArray();
        completeReading(in, lastPosition);
    }

    /**
     * Copy constructor
     */
    public CarHvacProperty(CarHvacProperty that) {
        super(VERSION);
        mPropertyId = that.getPropertyId();
        mType = that.getType();
        mZone = that.getZone();
        mFloatMax = that.getFloatMax();
        mFloatMin = that.getFloatMin();
        mFloatValues = that.getFloatValues();
        mIntMax = that.getIntMax();
        mIntMin = that.getIntMin();
        mIntValues =  that.getIntValues();
    }
    /**
     * Constructor for a boolean property
     */
    public CarHvacProperty(int propertyId, int zone, boolean value) {
        super(VERSION);
        mPropertyId = propertyId;
        mType = CarHvacManager.PROPERTY_TYPE_BOOLEAN;
        mZone = zone;
        mIntMax = 1;
        mIntMin = 0;
        mIntValues = new int[] { value ? 1 : 0 };
        mFloatMax = 0;
        mFloatMin = 0;
    }


    /**
     * Constructor for a float property
     */
    public CarHvacProperty(int propertyId, int zone, float min, float max, float value) {
        super(VERSION);
        mPropertyId = propertyId;
        mType = CarHvacManager.PROPERTY_TYPE_FLOAT;
        mZone = zone;
        mFloatMax = min;
        mFloatMin = max;
        mFloatValues = new float[] {value};
        mIntValues = null;
        mIntMax = 0;
        mIntMin = 0;
    }

    /**
     * Constructor for an integer property
     */
    public CarHvacProperty(int propertyId, int zone, int min, int max, int value) {
        super(VERSION);
        mPropertyId = propertyId;
        mType = CarHvacManager.PROPERTY_TYPE_INT;
        mZone = zone;
        mIntMax = min;
        mIntMin = max;
        mIntValues = new int[] { value };
        mFloatMax = 0;
        mFloatMin = 0;
    }

    /**
     * Constructor for an integer vector property
     */
    public CarHvacProperty(int propertyId, int zone, int min, int max, int[] values) {
        super(VERSION);
        mPropertyId = propertyId;
        mType = CarHvacManager.PROPERTY_TYPE_INT_VECTOR;
        mZone = zone;
        mIntMax = min;
        mIntMin = max;
        mIntValues = Arrays.copyOf(values, values.length);
        mFloatMax = 0;
        mFloatMin = 0;
    }

    /**
     * Constructor for a float vector property
     */
    public CarHvacProperty(int propertyId, int zone, float min, float max, float[] values) {
        super(VERSION);
        mPropertyId = propertyId;
        mType = CarHvacManager.PROPERTY_TYPE_FLOAT_VECTOR;
        mZone = zone;
        mFloatMax = min;
        mFloatMin = max;
        mFloatValues = Arrays.copyOf(values, values.length);
        mIntMax = 0;
        mIntMin = 0;
    }

    // Getters.
    public int   getPropertyId()     { return mPropertyId; }
    public int   getType()           { return mType; }
    public int   getZone()           { return mZone; }
    public boolean getBooleanValue() { return mIntValues[0] == 1; }
    public float getFloatMax()       { return mFloatMax; }
    public float getFloatMin()       { return mFloatMin; }
    public float getFloatValue()     { return mFloatValues[0]; }
    public float[] getFloatValues()  { return mFloatValues; }
    public int   getIntMax()         { return mIntMax; }
    public int   getIntMin()         { return mIntMin; }
    public int   getIntValue()       { return mIntValues[0]; }
    public int[] getIntValues()      { return mIntValues; }

    // Setters.
    public void setBooleanValue(boolean value) {
        assertType(CarHvacManager.PROPERTY_TYPE_BOOLEAN);
        mIntValues[0] = value ? 1 : 0;
    }
    public void setFloatValue(float value) {
        assertType(CarHvacManager.PROPERTY_TYPE_FLOAT);
        mFloatValues[0] = value;
    }
    public void setIntValue(int value) {
        assertType(CarHvacManager.PROPERTY_TYPE_INT);
        mIntValues[0] = value;
    }
    public void setIntValues(int[] values) {
        assertType(CarHvacManager.PROPERTY_TYPE_INT_VECTOR);
        mIntValues = Arrays.copyOf(values, values.length);
    }
    public void setFloatValues(float[] values) {
        assertType(CarHvacManager.PROPERTY_TYPE_INT_VECTOR);
        mFloatValues = Arrays.copyOf(values, values.length);
    }
    public void setZone(int zone)   { mZone  = zone; }

    @Override
    public String toString() {
        String myString = "mPropertyId: "  + mPropertyId + "\n" +
                          "mType:       "  + mType       + "\n" +
                          "mZone:       "  + mZone       + "\n";
        switch (mType) {
            case CarHvacManager.PROPERTY_TYPE_BOOLEAN:
                myString += "mIntValue:   "  + mIntValues[0] + "\n";
                break;
            case CarHvacManager.PROPERTY_TYPE_FLOAT:
                myString += "mFloatMax:   "  + mFloatMax   + "\n" +
                            "mFloatMin:   "  + mFloatMin   + "\n" +
                            "mFloatValue: "  + mFloatValues[0] + "\n";
                break;
            case CarHvacManager.PROPERTY_TYPE_INT:
                myString += "mIntMax:     "  + mIntMax     + "\n" +
                            "mIntMin:     "  + mIntMin     + "\n" +
                            "mIntValue:   "  + mIntValues[0] + "\n";
                break;
            case CarHvacManager.PROPERTY_TYPE_INT_VECTOR:
                myString += "mIntMax:     "  + mIntMax     + "\n" +
                            "mIntMin:     "  + mIntMin     + "\n" +
                            "mIntValues:  "  + Arrays.toString(mIntValues) + "\n";
                break;
            case CarHvacManager.PROPERTY_TYPE_FLOAT_VECTOR:
                myString += "mFloatMax:     "  + mFloatMax     + "\n" +
                            "mFloatMin:     "  + mFloatMin     + "\n" +
                            "mFloatValues:  "  + Arrays.toString(mFloatValues) + "\n";
                break;
            default:
                throw new IllegalArgumentException();
        }

        return myString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CarHvacProperty that = (CarHvacProperty) o;
        return mPropertyId == that.mPropertyId &&
                mType == that.mType &&
                mZone == that.mZone &&
                Float.compare(that.mFloatMax, mFloatMax) == 0 &&
                Float.compare(that.mFloatMin, mFloatMin) == 0 &&
                mIntMax == that.mIntMax &&
                mIntMin == that.mIntMin &&
                Arrays.equals(mFloatValues, that.mFloatValues) &&
                Arrays.equals(mIntValues, that.mIntValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mPropertyId,
                mType,
                mZone,
                mFloatMax,
                mFloatMin,
                mFloatValues,
                mIntMax,
                mIntMin,
                mIntValues);
    }

    private void assertType(int expectedType) {
        if (mType != expectedType) {
            throw new IllegalArgumentException(
                    "Expected type: " + expectedType + ", actual type: " + mType);
        }
    }
}