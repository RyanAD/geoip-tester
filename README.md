# Build / Run
To be documented

# Endpoints

`/lookup?ipAddress=<ip>&expectedAddress=<address>`

Looks up the ip address with all configured GeoIP providers. (ip, address) pairs are cached and won't be looked up again.

* ipAddress - The ip address you want to lookup
* expectedAddress - The physical address for the ip address. Currently this should only be an address. In the future
a full address will be supported for distance calculations
  
`/stats`

Returns stats on each provider including accurary and latency percentiles

`/dump`

Dumps every lookup made so far