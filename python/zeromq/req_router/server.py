import zmq


class Server(object):

    def __init__(self, server_port=20110):
        self.ctx = zmq.Context()
        self.url = 'tcp://127.0.0.1:{0}'.format(server_port)

    def run(self):
        socket = self.ctx.socket(zmq.ROUTER)
        socket.bind(self.url)
        delayed_msg = None
        server_accum = 0
        while True:

            if delayed_msg is None:
                delayed_msg = socket.recv_multipart()
            else:
                ident, empty, msg = socket.recv_multipart()
                server_accum += 1
                socket.send_multipart([ident, '', 'Server received {0} : Server Msg [{1}]'.format(msg, server_accum)])
                (ident, empty, msg), delayed_msg = delayed_msg, None
                server_accum += 1
                socket.send_multipart([ident, '', 'Server received {0} : Server Msg [{1}]'.format(msg, server_accum)])


def main():
    server = Server()
    server.run()

if __name__ == '__main__':
    main()