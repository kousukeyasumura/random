import time
from threading import Thread

def this():
    print("Starting this...")
    time.sleep(2)
    print("Did this!")

def that():
    print("Starting that...")
    time.sleep(3)
    print("Did that!")


t1 = Thread(target=this)
t1.start()

t2 = Thread(target=that)
t2.start()

