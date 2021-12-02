/*
  Copyright 2012 Sébastien Vrillaud
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package de.sualk1000.indoorcycler;

import android.content.Context;

import com.garmin.fit.DateTime;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;


import java.io.File;
import java.util.Date;
import java.util.LinkedList;

public class Export {
  
  public static File buildFitFile(Context context, LinkedList<Mesg> measurements) throws StorageNotMountedException {


    String filename = "MyCache.fit";

    File file = new File(context.getCacheDir() , filename);

    if(file.exists())
      file.delete();

    FileEncoder encoder = new FileEncoder(file);

    FileIdMesg fileIdMesg = new FileIdMesg();
    fileIdMesg.setType(com.garmin.fit.File.ACTIVITY);
    fileIdMesg.setManufacturer(Manufacturer.TANITA);
    fileIdMesg.setProduct(1);
    fileIdMesg.setSerialNumber(1L);
    encoder.write(fileIdMesg);

    DeviceInfoMesg deviceInfoMesg = new DeviceInfoMesg();
    deviceInfoMesg.setDeviceIndex((short) 1);
    deviceInfoMesg.setManufacturer(Manufacturer.TANITA);
    deviceInfoMesg.setProduct(1);
    deviceInfoMesg.setProductName("FIT Cookbook"); // Max 20 Chars
    deviceInfoMesg.setSerialNumber(1L);
    deviceInfoMesg.setSoftwareVersion((float) 1L);
    deviceInfoMesg.setTimestamp(new DateTime(new Date().getTime()));

    encoder.write(deviceInfoMesg);

    for (Mesg measurement : measurements) {

      encoder.write(measurement);
    }

    encoder.close();
    return file;
  }


}
