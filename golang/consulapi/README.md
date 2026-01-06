Consul API example

This is a tiny, isolated example that shows how to use the HashiCorp Consul Go API to write and read a KV pair.

Requirements
- Go 1.20+
- (Optional) A local Consul agent running and accessible at the default address (127.0.0.1:8500). If no Consul agent is available, the example will attempt to connect and may fail.

How to build

Run these in the `consulapi` directory:

```bash
# fetch dependencies and build
go build
# run
./consulapi-example  # binary will be named after module directory (consulapi)
```

Notes
- The example is intentionally small. If you don't have a running Consul agent, consider running a local dev agent with `consul agent -dev` (install Consul separately).
