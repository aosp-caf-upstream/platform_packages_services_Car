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
package com.android.car.hal;

import android.os.RemoteException;
import android.util.Log;

import com.android.car.CarLog;

import com.android.car.hardware.hvac.CarHvacEvent;
import com.android.car.hardware.hvac.CarHvacManager;
import com.android.car.hardware.hvac.CarHvacProperty;

import com.android.car.vehiclenetwork.VehicleNetworkConsts;
import com.android.car.vehiclenetwork.VehicleNetworkConsts.VehicleValueType;
import com.android.car.vehiclenetwork.VehicleNetworkProto.VehiclePropConfig;
import com.android.car.vehiclenetwork.VehicleNetworkProto.VehiclePropConfigs;
import com.android.car.vehiclenetwork.VehicleNetworkProto.VehiclePropValue;
import com.android.car.vehiclenetwork.VehiclePropValueUtil;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class HvacHalService extends HalServiceBase {
    private static final boolean   DBG = true;
    private static final String    TAG = CarLog.TAG_HVAC + ".HvacHalService";
    private HvacHalListener        mListener;
    private List<VehiclePropValue> mQueuedEvents;
    private final VehicleHal       mVehicleHal;
    private final HashMap<Integer, CarHvacProperty> mProps =
            new HashMap<Integer, CarHvacProperty>();

    public interface HvacHalListener {
        void onPropertyChange(CarHvacEvent event);
        void onError(int zone, int property);
    }

    public HvacHalService(VehicleHal vehicleHal) {
        mVehicleHal = vehicleHal;
        if (DBG) {
            Log.d(TAG, "started HvacHalService!");
        }
    }

    public void setListener(HvacHalListener listener) {
        List<VehiclePropValue> eventsToDispatch = null;
        synchronized (this) {
            mListener = listener;
            if (mQueuedEvents != null) {
                eventsToDispatch = mQueuedEvents;
                mQueuedEvents = null;
            }
        }
        if (eventsToDispatch != null) {
            dispatchEventToListener(listener, eventsToDispatch);
        }
    }

    public List<CarHvacProperty> getHvacProperties() {
        List<CarHvacProperty> propList = null;
        synchronized (mProps) {
            propList = new ArrayList<CarHvacProperty>(mProps.values());
        }
        return propList;
    }

    public CarHvacProperty getHvacProperty(int prop, int zone) {
        int halProp = hvacToHalPropId(prop);
        CarHvacProperty  hvacProp = null;
        VehiclePropValue getValue = null;

        synchronized (mProps) {
            hvacProp = new CarHvacProperty(mProps.get(halProp));
        }

        switch(hvacProp.getType()) {
            case CarHvacManager.PROPERTY_TYPE_BOOLEAN:
                getValue = VehiclePropValueUtil.createZonedBooleanValue(halProp, zone, false, 0);
                break;
            case CarHvacManager.PROPERTY_TYPE_FLOAT:
                getValue = VehiclePropValueUtil.createZonedFloatValue(halProp, zone, 0, 0);
                break;
            case CarHvacManager.PROPERTY_TYPE_INT:
                getValue = VehiclePropValueUtil.createZonedIntValue(halProp, zone, 0, 0);
                break;
        }

        VehiclePropValue value = mVehicleHal.getVehicleNetwork().getProperty(getValue);

        if(value != null) {
            switch(hvacProp.getType()) {
                case CarHvacManager.PROPERTY_TYPE_BOOLEAN:
                    hvacProp.setBooleanValue(value.getZonedValue().getInt32Value() == 1);
                    break;
                case CarHvacManager.PROPERTY_TYPE_INT:
                    hvacProp.setIntegerValue(value.getZonedValue().getInt32Value());
                    break;
                case CarHvacManager.PROPERTY_TYPE_FLOAT:
                    hvacProp.setFloatValue(value.getZonedValue().getFloatValue());
                    break;
            }
            hvacProp.setZone(zone);
        } else {
            hvacProp = null;
        }

        return hvacProp;
    }

    public void setHvacProperty(CarHvacProperty prop) {
        int halProp = hvacToHalPropId(prop.getPropertyId());

        switch(prop.getType()) {
            case CarHvacManager.PROPERTY_TYPE_BOOLEAN:
                mVehicleHal.getVehicleNetwork().setZonedBooleanProperty(halProp, prop.getZone(),
                        prop.getIntValue() == 1);
                break;
            case CarHvacManager.PROPERTY_TYPE_INT:
                mVehicleHal.getVehicleNetwork().setZonedIntProperty(halProp, prop.getZone(),
                        prop.getIntValue());
                break;
            case CarHvacManager.PROPERTY_TYPE_FLOAT:
                mVehicleHal.getVehicleNetwork().setZonedFloatProperty(halProp, prop.getZone(),
                        prop.getFloatValue());
                break;
            default:
                throw new IllegalArgumentException();
        }
        return;
    }

    @Override
    public void init() {
        if (DBG) {
            Log.d(TAG, "init()");
        }
        synchronized (mProps) {
            // Subscribe to each of the HVAC properties
            for (Integer prop : mProps.keySet()) {
                mVehicleHal.subscribeProperty(this, prop, 0);
            }
        }
    }

    @Override
    public void release() {
        if (DBG) {
            Log.d(TAG, "release()");
        }
        synchronized (mProps) {
            for (Integer prop : mProps.keySet()) {
                mVehicleHal.unsubscribeProperty(this, prop);
            }

            // Clear the property list
            mProps.clear();
        }
        mListener = null;
    }

    @Override
    public synchronized List<VehiclePropConfig> takeSupportedProperties(
            List<VehiclePropConfig> allProperties) {
        List<VehiclePropConfig> taken = new LinkedList<VehiclePropConfig>();

        for (VehiclePropConfig p : allProperties) {
            int prop = p.getProp();
            int hvacPropId = 0;

            try {
                hvacPropId = halToHvacPropId(prop);
            } catch (IllegalArgumentException e) {
                // This parameter is not handled by HvacService
                continue;
            }

            if (hvacPropId != 0) {
                CarHvacProperty hvacProp = null;
                int halType = p.getValueType();
                int valZone = p.getConfigArray(0);

                switch(halType) {
                    case VehicleValueType.VEHICLE_VALUE_TYPE_ZONED_BOOLEAN:
                        hvacProp = new CarHvacProperty(hvacPropId, valZone, false);
                        break;
                    case VehicleValueType.VEHICLE_VALUE_TYPE_ZONED_FLOAT: {
                        float valMin = p.getFloatMin();
                        float valMax = p.getFloatMax();
                        hvacProp = new CarHvacProperty(hvacPropId, valZone, valMin, valMax, 0);
                        break;
                    }
                    case VehicleValueType.VEHICLE_VALUE_TYPE_ZONED_INT32: {
                        int valMin = p.getInt32Min();
                        int valMax = p.getInt32Max();
                        hvacProp = new CarHvacProperty(hvacPropId, valZone, valMin, valMax, 0);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(TAG + ": halType " + halType + " not" +
                                "handled!");
                }

                taken.add(p);
                mProps.put(prop, hvacProp);
                if (DBG) {
                    Log.d(TAG, "takeSupportedProperties:  " + prop);
                }
            }
        }
        return taken;
    }

    @Override
    public void handleHalEvents(List<VehiclePropValue> values) {
        HvacHalListener listener = null;
        synchronized (this) {
            listener = mListener;
            if (listener == null) {
                if (mQueuedEvents == null) {
                    mQueuedEvents = new LinkedList<VehiclePropValue>();
                }
                mQueuedEvents.addAll(values);
            }
        }
        if (listener != null) {
            dispatchEventToListener(listener, values);
        }
    }

    private void dispatchEventToListener(HvacHalListener listener, List<VehiclePropValue> values) {
        for (VehiclePropValue v : values) {
            int prop = v.getProp();
            int hvacPropId = halToHvacPropId(prop);

            // TODO:  What is the interface for returning a failed command back to the manager?
            if (hvacPropId != 0) {
                CarHvacEvent event = null;
                int halType = v.getValueType();
                int zone = v.getZonedValue().getZoneOrWindow();

                switch(halType) {
                    case VehicleValueType.VEHICLE_VALUE_TYPE_ZONED_BOOLEAN:
                        event = new CarHvacEvent(CarHvacEvent.HVAC_EVENT_PROPERTY_CHANGE,
                                hvacPropId, zone, v.getZonedValue().getInt32Value() == 1);
                        break;
                    case VehicleValueType.VEHICLE_VALUE_TYPE_ZONED_FLOAT: {
                        event = new CarHvacEvent(CarHvacEvent.HVAC_EVENT_PROPERTY_CHANGE,
                                hvacPropId, zone, v.getZonedValue().getFloatValue());
                        break;
                    }
                    case VehicleValueType.VEHICLE_VALUE_TYPE_ZONED_INT32: {
                        event = new CarHvacEvent(CarHvacEvent.HVAC_EVENT_PROPERTY_CHANGE,
                                hvacPropId, zone, v.getZonedValue().getInt32Value());
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(TAG + ": halType " + halType + " not" +
                                "handled!");
                }

                listener.onPropertyChange(event);
                if (DBG) {
                    Log.d(TAG, "handleHalEvents prop = " + prop + "  zone = " + zone +
                            "  intVal = " + event.getIntValue() + " floatVal = " +
                            event.getFloatValue());
                }
            } else if (DBG) {
                Log.d(TAG, "handleHalEvents - unknown property" + prop);
            }
        }
    }

    @Override
    public void dump(PrintWriter writer) {
        writer.println("*HVAC HAL*");
        writer.println("  Properties available:");
        for (Integer prop : mProps.keySet()) {
            writer.println("    " + prop);
        }
    }

    // Convert the HVAC public API property ID to HAL property ID
    private static int hvacToHalPropId(int hvacPropId) {
        int halPropId = 0;
        switch (hvacPropId) {
            case CarHvacManager.HVAC_ZONED_FAN_SPEED_SETPOINT:
                halPropId = VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_FAN_SPEED;
                break;
            case CarHvacManager.HVAC_ZONED_FAN_POSITION:
                halPropId = VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_FAN_DIRECTION;
                break;
            case CarHvacManager.HVAC_ZONED_TEMP_ACTUAL:
                halPropId = VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_TEMPERATURE_CURRENT;
                break;
            case CarHvacManager.HVAC_ZONED_TEMP_SETPOINT:
                halPropId = VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_TEMPERATURE_SET;
                break;
            case CarHvacManager.HVAC_WINDOW_DEFROSTER_ON:
                halPropId = VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_DEFROSTER;
                break;
            case CarHvacManager.HVAC_AC_ON:
                halPropId = VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_AC_ON;
                break;
        }
        if (halPropId == 0) {
            throw new IllegalArgumentException("hvacPropId " + hvacPropId + " is not supported");
        }
        return halPropId;
    }

    // Convert he HAL specific property ID to HVAC public API
    private static int halToHvacPropId(int halPropId) {
        int hvacPropId = 0;
        switch (halPropId) {
            case VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_FAN_SPEED:
                hvacPropId = CarHvacManager.HVAC_ZONED_FAN_SPEED_SETPOINT;
                break;
            case VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_FAN_DIRECTION:
                hvacPropId = CarHvacManager.HVAC_ZONED_FAN_POSITION;
                break;
            case VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_TEMPERATURE_CURRENT:
                hvacPropId = CarHvacManager.HVAC_ZONED_TEMP_ACTUAL;
                break;
            case VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_TEMPERATURE_SET:
                hvacPropId = CarHvacManager.HVAC_ZONED_TEMP_SETPOINT;
                break;
            case VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_DEFROSTER:
                hvacPropId = CarHvacManager.HVAC_WINDOW_DEFROSTER_ON;
                break;
            case VehicleNetworkConsts.VEHICLE_PROPERTY_HVAC_AC_ON:
                hvacPropId = CarHvacManager.HVAC_AC_ON;
                break;
        }
        if (hvacPropId == 0) {
            throw new IllegalArgumentException("halPropId " + halPropId + " is not supported");
        }
        return hvacPropId;
    }
}