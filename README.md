Documentation Assistant Lamp (DA Lamp)
===
DA Lamp is a robotic lamp for makers or designers to automatically take pictures of every crucial step and upload to personal documentation database.

![](http://imgur.com/t3klscH.jpg)

It is made of laser-cut frames, 3D printed components and casting servo arms. It is low-cost, aachievable and rapid to make DA Lamp from the open-source design in every makerspaces and fablabs.

DA Lamp has 5 degrees of freedom to move autonomously, and the master system is based on Android. The mobile phone can be mounted on the lampshade, and the built-in camera is capable of recording in the meantime.

The softwarea app is under development, including the framework of object recognition and machine learning.

##Software

### app

For master control, it is currently based on Android system. It is under active development.

##Hardware

### design

Including STL and laser-cut frame files

### module

 * satshakit - translating the Serial commands from Android into I2C
 * servo - accepting I2C commands from satshakit and generating PWM signal to servo

## License
The MIT License
