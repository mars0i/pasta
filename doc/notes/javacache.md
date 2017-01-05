I was getting different results on my two machines, which seemingly
differed only sub version of Java 8.  I had a more recent version on
computer A.  When I ran my Mason model, it didn't seem to reflect the
latest code in the version of Mason that I'd compiled, nor the latest of
my own source code.  Very wierd.  I reinstalled Java.  No change.

Finally I followed instructions to clear the Java cache
from this page:
http://docs.oracle.com/javase/8/docs/technotes/guides/install/mac_install_faq.html#A1097080


	How do I clear the Java cache?
	
	A: Follow these steps:
	
	    From System Preferences, launch the Java Control Panel by clicking the Java icon in the Other section.
	
	    From the Java Control Panel, click Settings in the Temporary Internet Files section of the General tab.
	
	    In the Temporary Files Settings window, click Delete File.
	
	    From the Delete Files and Applications dialog, select Cached Applications and Applets and click OK to clear those files from the cache.
	
	To clear the applet and Web Start cache from a Terminal window, use the following command (note escaped space character):
	
	% rm -rf ~/Library/Application\ Support/Oracle/Java/Deployment/cache


To my surprise, this fixed the problem!
