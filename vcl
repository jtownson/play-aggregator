/*
*
* First, set up a backend to answer the request if there's not a cache hit.
*
*/
backend default {

    # Set a host.
    .host = "127.0.0.1";

    # Set a port. 80 is normal Web traffic.
    .port = "9000";
}
/*
*
* Next, configure the "receive" subroutine.
*
*/
sub vcl_recv {

    # Use the backend we set up above to answer the request if it's not cached.
    set req.backend = default;

    # Pass the request along to lookup to see if it's in the cache.
    return(lookup);
}
/*
*
* Next, let's set up the subroutine to deal with cache misses.
*
*/
sub vcl_miss {

    # We're not doing anything fancy. Just pass the request along to the
    # subroutine which will fetch something from the backend.
    return(fetch);
}
/*
*
* Now, let's set up a subroutine to deal with cache hits.
*
*/
sub vcl_hit {

    # Again, nothing fancy. Just pass the request along to the subroutine
    # which will deliver a result from the cache.
    return(deliver);
}
/*
*
* This is the subroutine which will fetch a response from the backend.
* It's pretty fancy because this is where the basic logic for caching is set.
*
*/
sub vcl_fetch {

    set beresp.do_esi = true;

    # Get the response. Set the cache lifetime of the response to 1 hour.
    #set beresp.ttl = 1h;

    # Indicate that this response is cacheable. This is important.
    set beresp.http.X-Cacheable = "YES";

    # Some backends *cough* Django *cough* will assign a Vary header for
    # each User-Agent which visits the site. Varnish will store a separate
    # copy of the page in the cache for each instance of the Vary header --
    # one for each User-Agent which visits the site. This is bad. So we're
    # going to strip away the Vary header.
    unset beresp.http.Vary;

    # Now pass this backend response along to the cache to be stored and served.
    return(deliver);


}
/*
*
* Finally, let's set up a subroutine which will deliver a response to the client.
*
*/
sub vcl_deliver {

    # Nothing fancy. Just deliver the goods.
    # Note: Both cache hits and cache misses will use this subroutine.
    return(deliver);
}