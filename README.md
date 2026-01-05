# Notifier
A very simple Android app. It allows for the creation of Notifiers. 
Notifiers just send the device notifications at desired times on desired times on repeat. 
There are a few customization options, background color of notification, title, description, and image on notification.

### Usage
Usage is fairly simple, just press the add a notifier button. 
The only note is that the color value actually works with alpha values, just add two hex digits to the start to change the alpha, e.g. #FF010203

### Notice
It uses exact time for the alarm manager so it may have an impact on battery life.

It also cannot do one time notifiers. If no days are selected not notification will go through.

If having issues with Notifiers not going off until the app is reopened, try going to: 
settings > apps > all apps > Notifier > app battery usage > allow background usage > select unrestricted.
It *might* help. 
