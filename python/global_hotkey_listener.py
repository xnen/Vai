import socket
import time
from pynput import keyboard

HOST = '127.0.0.1'
PORT = 12345
INTEGRATION_SALT = "YourSuperSecretSalt"
DOUBLE_PRESS_THRESHOLD = 0.3  # seconds

last_shift_time = 0
modifier_state = set()  # Track currently pressed modifier keys

def send_command(command):
    try:
        with socket.create_connection((HOST, PORT), timeout=2) as sock:
            # Send the salt and command, each terminated by a newline
            sock.sendall((INTEGRATION_SALT + "\n").encode('utf-8'))
            sock.sendall((command + "\n").encode('utf-8'))
            print("Sent command:", command)
    except Exception as e:
        print("Error sending command:", command, e)

def on_press(key):
    global last_shift_time
    try:
        if key == keyboard.Key.shift_l:
            current_time = time.time()
            # Check if this is a double-press of the left Shift key
            if current_time - last_shift_time <= DOUBLE_PRESS_THRESHOLD:
                if keyboard.Key.ctrl_l in modifier_state:
                    send_command("open-dialog")
                elif keyboard.Key.alt_l in modifier_state:
                    send_command("open-dialog-audio")
                # Reset to avoid multiple triggers
                last_shift_time = 0
            else:
                last_shift_time = current_time
        if key in (keyboard.Key.ctrl_l, keyboard.Key.alt_l):
            modifier_state.add(key)
    except Exception as e:
        print("Error in on_press:", e)

def on_release(key):
    if key in (keyboard.Key.ctrl_l, keyboard.Key.alt_l):
        modifier_state.discard(key)

def main():
    # Start the hotkey listener in the background with no visible window.
    with keyboard.Listener(on_press=on_press, on_release=on_release) as listener:
        listener.join()

if __name__ == '__main__':
    main()
