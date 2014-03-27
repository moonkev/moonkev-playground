import zmq
import sys


class Client(object):

    def __init__(self, identity, server_port=20110):
        self.ctx = zmq.Context()
        self.identity = 'client-{0}'.format(identity)
        self.url = 'tcp://127.0.0.1:{0}'.format(server_port)

    def run(self):
        i = 0
        while True:
            i += 1
            socket = self.ctx.socket(zmq.REQ)
            socket.connect(self.url)
            socket.identity = self.identity
            socket.send_string('Message {0} from {1}'.format(i, self.identity))
            msg = socket.recv()
            print msg
            socket.close()


def main():
    client = Client(sys.argv[1] if len(sys.argv) > 1 else 1)
    client.run()

if __name__ == '__main__':
    main()