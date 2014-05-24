# Play Aggregator (of business and CMS data) 

## Some Rationale

The existing BMC journalsites pages exhibit quite a bit of code that looks like the following
 
    #if(site.isPortal)
       #some markup
    #else
       #some different markup
    #

Have you ever noticed that 90% of all controllers in 90% of all web applications in the world do pretty much the same thing?
They all duplicate the logic of loading business data and returning a page with a 200 response. For instance:

    someBeans = getSomeBusinessData()
    aTin = getPageView()
   
    return Ok(aTin, someBeans)
   

Why do people insist on writing a new controller for every new endpoint? Oh, Sun Microsystems J2EE tutorial and your spawn, 
you have so much to answer for!

In a website project, like Oscar, where we plan to define our site structures and their content in a CMS,
it would certainly be unwise to then duplicate those structures (as webapp controller/view hierarchies). In doing
so, we require that changes to site structure be coordinated across application and CMS tiers. 
This is inflexible in practice and could lead us back to dark constructs like...

    #if(site.isPortal)
       #some markup
    #else
       #some different markup
    #
    
What can we do to solve this problem?

## The Spike

This demo explores a potential solution that has three key features

- The webapp as an abstract aggregator, with one controller for all endpoints (i.e., all endpoints that share the
_Ok(aTin, someBeans)_ implementation above).
- All site content and *markup* schemes defined in the CMS. This affords all the flexibility offered by the CMS (of sharing/forking markup 
between pages/sites). This avoids hardcoding a snapshot of this scheme in a webapp.
- REST-based business data coming, as *json*, from HTTP service components. The *HTML* rendering/markup for the business data is also defined in the CMS.
The business tier in this demo maintains the <esi> or <html> style shown in Chris James's play-esi. (As far as the aggregator is 
concerned, the business tier could be any REST API on the internet).

For me, the spike also served for learning more about Scala and Play. Apache velocity is used as the template language,
so it shows how to implement a template plugin for Play.  

## Technologies

- Play framework 2.2.1, with velocity as the templating language (using an [Furyu's Apache Velocity plugin](https://github.com/Furyu/play-velocity-plugin))
- Varnish for cacheing
- Apache Velocity

## To run

- Install play and scala
- Fork or download this project
- Put the following entry in your hosts file
    127.0.0.1		biomedcentral.com
- Commands...
`$ cd play-aggregator`
`$ play run`
- Browse to
`http://biomedcentral.com:9000/index`
- Explore the code to see what's going on (see below)

## Notes

## Potential problems: coherence



