#Credits: The original author is Maarten Smeets
#this is a "fork" from his original script which can be found here: https://dl.dropboxusercontent.com/u/6693935/blog/NexusArtifactRemoverNetBeansProject.zip
#his blog article about this script can be found here: http://www.sonatype.org/nexus/2015/07/13/sonatype-nexus-delete-artifacts-based-on-a-selection/

import xml.etree.ElementTree as ET
import httplib
import datetime
import string
import base64
import getpass

__author__ = "maarten"
__date__ = "$Jul 11, 2015 12:26:03 PM$"

#how to access Nexus. used to build the URL in get_nexus_artifact_version_listing and get_nexus_artifact_names
NEXUSHOST = ""
NEXUSPORT = "443"
USE_SSL = True
NEXUSREPOSITORY = "releases"
NEXUSBASEURL = "/nexus/service/local/repositories/"

#what to delete
ARTIFACTGROUP = "foo.bar" #required
ARTIFACTNAME = None #can be an artifact name or None. None first searches for artifacts in the group
ARTIFACTVERSIONMIN = "2.4.0" #can be None or a version like 1.1
ARTIFACTVERSIONMAX = "2.4.0" #can be None or a version like 1.2
ARTIFACTMAXLASTMODIFIED = None #datetime.datetime.strptime("2015-10-29 12:00:00","%Y-%m-%d %H:%M:%S") #can be None or datetime in format like 2014-10-29 12:00:00
ARTIFACTMINLASTMODIFIED = None #datetime.datetime.strptime("2014-10-28 12:00:00","%Y-%m-%d %H:%M:%S") #can be None or datetime in format like 2014-10-28 12:00:00

#generates URL based on constants and artifactname, calls Nexus and returns an ElementTree
#example URL generated http://localhost:8081/nexus/service/local/repositories/releases/content/nl/amis/smeetsm/application/testproject/
def get_nexus_artifact_version_listing(artifactname):    
    if (USE_SSL):
        conn = httplib.HTTPSConnection(NEXUSHOST, NEXUSPORT)
    else:
        conn = httplib.HTTPConnection(NEXUSHOST, NEXUSPORT)
    
    userAndPass = string.strip(base64.encodestring(NEXUSUSERNAME + ':' + NEXUSPASSWORD))
    headers = { 'Authorization' : 'Basic %s' %  userAndPass }
    
    url = NEXUSBASEURL+NEXUSREPOSITORY+"/content/"+ARTIFACTGROUP.replace(".","/")+"/"+artifactname+"/"
    #print "URL to determine artifact versions: "+""+NEXUSHOST+":"+NEXUSPORT+url
    conn.request("GET", url, headers=headers)
    response = conn.getresponse()
    if (response.status == 200):
        return ET.fromstring(response.read())
    else:
        print "error: " ,response.status
        return None

#generates URL based on constants, calls Nexus and returns an ElementTree
#example URL generated http://localhost:8081/nexus/service/local/repositories/releases/content/nl/amis/smeetsm/application/
def get_nexus_artifact_names():    
    if (USE_SSL):
        conn = httplib.HTTPSConnection(NEXUSHOST, NEXUSPORT)
    else:
        conn = httplib.HTTPConnection(NEXUSHOST, NEXUSPORT)    
    
    userAndPass = string.strip(base64.encodestring(NEXUSUSERNAME + ':' + NEXUSPASSWORD))
    headers = { 'Authorization' : 'Basic %s' %  userAndPass }
    url = NEXUSBASEURL+NEXUSREPOSITORY+"/content/"+ARTIFACTGROUP.replace(".","/")+"/"
    #print "URL to determine artifact names: "+NEXUSHOST+":"+NEXUSPORT+url
    conn.request("GET", url, headers=headers)
    response = conn.getresponse()
    if (response.status == 200):
        return ET.fromstring(response.read())
    else:
        print "error: " ,response.status
        return None

#evaluates an item. returns True if it is selected and False if it isn't. evaluates lastModified and version
#sample XML content_item:
#<content-item>
#<resourceURI>http://localhost:8081/nexus/service/local/repositories/releases/content/nl/amis/smeetsm/application/testproject/1.1/</resourceURI>
#<relativePath>/nl/amis/smeetsm/application/testproject/1.1/</relativePath>
#<text>1.1</text>
#<leaf>false</leaf>
#<lastModified>2014-10-28 18:29:48.0 UTC</lastModified>
#<sizeOnDisk>-1</sizeOnDisk>
#</content-item>
def content_item_in_selection(content_item):
    relativePath = content_item.find("./relativePath").text
    lastmodified = content_item.find("./lastModified").text
    leaf = content_item.find("./leaf").text
    lastmodified_short = lastmodified[0:19]
    try:    
        lastmodified_dt = datetime.datetime.strptime(lastmodified_short,"%Y-%m-%d %H:%M:%S")
    except:
        print "Unable to parse "+lastmodified+" to a datetime in content_item_in_selection while determining if item should be in selection"
        raise

    #leaf is false in case of directories
    if (leaf == "false"):
        #print "Item datetime: "+lastmodified
        if (
        ((ARTIFACTMINLASTMODIFIED is not None and lastmodified_dt >= ARTIFACTMINLASTMODIFIED) or ARTIFACTMINLASTMODIFIED is None) 
        and ((ARTIFACTMAXLASTMODIFIED is not None and lastmodified_dt <= ARTIFACTMAXLASTMODIFIED) or ARTIFACTMAXLASTMODIFIED is None)
        ):
            lastmodified_in_selection = True
        else:
            lastmodified_in_selection = False
            
        version_str = content_item.find("./text").text
        #print "Item version: "+version_str
            
        if (
        ((ARTIFACTVERSIONMIN is not None and version_str >= ARTIFACTVERSIONMIN) or ARTIFACTVERSIONMIN is None) 
        and ((ARTIFACTVERSIONMAX is not None and version_str <= ARTIFACTVERSIONMAX) or ARTIFACTVERSIONMAX is None)
        ):
            version_in_selection = True
        else:
            version_in_selection = False
    
        if (lastmodified_in_selection and version_in_selection):
            return True
        else:
            return False
    else:
        return False
    
def remove_artifact(groupid,artifactid,version):
    print "Artifact to be removed "+groupid+": "+artifactid+": "+version
    url = NEXUSBASEURL+NEXUSREPOSITORY+"/content/"+groupid.replace(".","/")+"/"+artifactid+"/"+version
    print "Sending HTTP DELETE request to http://"+NEXUSHOST+":"+NEXUSPORT+url
    
    # from http://mozgovipc.blogspot.nl/2012/06/python-http-basic-authentication-with.html
    # base64 encode the username and password
    auth = string.strip(base64.encodestring(NEXUSUSERNAME + ':' + NEXUSPASSWORD))
    
    if (USE_SSL):
        service = httplib.HTTPS(NEXUSHOST,NEXUSPORT)
    else:
        service = httplib.HTTP(NEXUSHOST,NEXUSPORT)
    
    # write your headers
    service.putrequest("DELETE", url)
    service.putheader("Host", NEXUSHOST)
    service.putheader("User-Agent", "Python http auth")
    service.putheader("Content-type", "text/html; charset=\"UTF-8\"")
    # write the Authorization header like: 'Basic base64encode(username + ':' + password)
    service.putheader("Authorization", "Basic %s" % auth)
 
    service.endheaders()
    service.send("")
    # get the response
    statuscode, statusmessage, header = service.getreply()
    
    #No content (HTTP 204) is ok when an artifact is removed
    print "Response: ", statuscode, statusmessage
    #print "Headers: ", header
    res = service.getfile().read()
    #print 'Content: ', res

def remove_artifacts():
    if (ARTIFACTNAME is not None): 
        print "Processing artifact: "+ARTIFACTNAME
        artifact_versions = get_nexus_artifact_version_listing(ARTIFACTNAME)
        content_items = artifact_versions.findall('./data/content-item')
        for content_item in content_items:
            if content_item_in_selection(content_item):
                #print "Item in selection"
                remove_artifact(ARTIFACTGROUP,ARTIFACTNAME,content_item.find("./text").text)
    else:
        artifact_names = get_nexus_artifact_names()
        for artifact_name in artifact_names.findall("./data/content-item"):
            artifactname =  artifact_name.find("./text").text
            print "Processing artifact: "+artifactname
            artifact_versions = get_nexus_artifact_version_listing(artifactname)
            content_items = artifact_versions.findall('./data/content-item')
            for content_item in content_items:
                if content_item_in_selection(content_item):
                    #print "Item in selection"
                    remove_artifact(ARTIFACTGROUP,artifactname,content_item.find("./text").text)

NEXUSUSERNAME = raw_input('Username: ')
NEXUSPASSWORD = getpass.getpass('Password: ')
remove_artifacts()
