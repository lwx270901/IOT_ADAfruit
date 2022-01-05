import random
import requests
import time
import sys
from Adafruit_IO import MQTTClient

AIO_USERNAME = "izayazuna"
AIO_KEY = "aio_TyOi92YHzHMImC9BOpX4cB4VbOGa"

LED_INDEX = 1
RGB_INDEX = 1

APIKEY = "402953e62e07f763c7b5233dbcad9b58"
state = 1
next_state = 2
count = 0
feed = ""
value = ""

waiting_period = 3
sent_again = 0
timer_sent = 0


def convert(code):
    return code - 273


def get_temp(API_KEY):
    url = f"http://api.openweathermap.org/data/2.5/weather?q=London&appid=402953e62e07f763c7b5233dbcad9b58"

    response = requests.get(url).json()
    currentTemp = response['main']['temp']
    formattedTemp = '{:.0f}'.format(convert(currentTemp))
    return formattedTemp


def get_hum(API_KEY):
    url = f"http://api.openweathermap.org/data/2.5/weather?q=London&appid=402953e62e07f763c7b5233dbcad9b58"

    response = requests.get(url).json()
    currentHum = response['main']['humidity']
    formattedHum = '{:.0f}'.format(convert(currentHum))
    return formattedHum


def connected(client):
    print("Ket noi thanh cong...")
    client.subscribe("button")
    client.subscribe("door")
    client.subscribe("error")
    client.subscribe("temperature")
    client.subscribe("hummidity")


def subscribe(client, userdata, mid, granted_qos):
    print("Subscribe thanh cong...")


def disconnected(client):
    print("Ngat ket noi...")
    sys.exit(1)


def message(client, feed_id, payload):
    print("Nhan du lieu: " + payload)
    if str(feed_id) == "button":
        global LED_INDEX
        LED_INDEX = LED_INDEX * (-1)
        client.publish("error", "{ACK: " + str(LED_INDEX) + "} from feed" + feed_id)


    if str(feed_id) == "error":
        print("Send data to", feed, " Successfully")
        global sent_again
        global waiting_period
        global timer_sent
        timer_sent = 0
        sent_again = 0
        waiting_period = 3


client = MQTTClient(AIO_USERNAME, AIO_KEY)
client.on_connect = connected
client.on_disconnect = disconnected
client.on_message = message
client.on_subscribe = subscribe
client.connect()
client.loop_background()


def sent_message(feed, message):
    print("Sending", feed, message)
    client.publish(feed, message)


while True:
    if state == 1:  # read temperat
        value = get_temp(APIKEY)
        feed = "temperature"
        sent_again = 1
        state = -1
    elif state == 2:
        value = get_hum(APIKEY)
        feed = "hummidity"
        sent_again = 1
        state = -1
    elif state == 0:
        if count < 5:
            count += 1
        else:
            count = 0
            if next_state > 2:
                state = 1
                next_state = 2
            else:
                state = next_state
                next_state += 1
    elif state == -1:
        if sent_again == 1:
            if timer_sent < 3:
                timer_sent += 1
            else:
                timer_sent = 0
                sent_message(feed, value)
                waiting_period -= 1
            if waiting_period == 0:
                waiting_period = 3
                sent_again = 0
                timer_sent = 0
                print("failed")
        else:
            state = 0

    time.sleep(1)
