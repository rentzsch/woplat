# Recipe for Installing WebObjects 5.3.3 on Mac OS X 10.6 Snow Leopard (non-Server)

## Deployment

### WebObjects 5.3.3

Use WOInstaller to download WebObjects 5.3.3.

	curl -O http://webobjects.mdimension.com/wolips/WOInstaller.jar
	mkdir wo533
	mkdir wo543
	java -jar WOInstaller.jar 5.3.3 wo533
	java -jar WOInstaller.jar 5.4.3 wo543

Install crucial WebObjects frameworks:

	pushd wo533/System/Library/Frameworks/
	sudo cp -R \
		JavaEOAccess.framework \
		JavaEOControl.framework \
		JavaFoundation.framework \
		JavaJDBCAdaptor.framework \
		JavaWebObjects.framework \
		JavaXML.framework \
		/System/Library/Frameworks
	cd ../PrivateFrameworks
	sudo cp -R JavaMonitor.framework /System/Library/PrivateFrameworks
	popd

Install crucial WebObjects support folder:

	cp -R wo533/Library/WebObjects /Library
	mkdir /Library/WebObjects/Configuration
	sudo chown -R appserver /Library/WebObjects/Configuration

### Apache, wotaskd, JavaMonitor

Install WebObjects 5.3.3 deployment infrastructure:

	sudo cp -R wo533/System/Library/WebObjects /System/Library
	pushd /System/Library/WebObjects/JavaApplications
	sudo chmod ugo+x wotaskd.woa/wotaskd JavaMonitor.woa/JavaMonitor
	popd

Selectively install WebObject 5.4.3's Apache 2.2 adapter, since Snow Leopard comes with Apache 2.2:

	sudo cp -R wo543/System/Library/WebObjects/Adaptors/Apache2.2 \
		/System/Library/WebObjects/Adaptors

But WebObject 5.4.3's Apache 2.2 adapter, targeted for 10.5, SEGFAULTs on 10.6. So replace the `.so` with one that was compiled for 10.6:

	curl -O 'http://webobjects.mdimension.com/wonder/mod_WebObjects/Apache2.2/macosx/10.6/mod_WebObjects.so'
	sudo mv mod_WebObjects.so /System/Library/WebObjects/Adaptors/Apache2.2

Add WebObjects to `/etc/apache2/httpd.conf`:

	    <IfModule ssl_module>
	    	SSLRandomSeed startup builtin
	    	SSLRandomSeed connect builtin
	    </IfModule>
	
	+   Include /System/Library/WebObjects/Adaptors/Apache2.2/apache.conf
	
	    Include /private/etc/apache2/other/*.conf

Load the Apache module:

	sudo apachectl configtest
	sudo apachectl restart # or apachectl graceful or apachectl start

Introduce wotaskd to launchd:

	curl -O 'http://cloud.github.com/downloads/rentzsch/woplat/com.apple.wotaskd.plist'
	sudo chown root com.apple.wotaskd.plist
	sudo chgrp wheel com.apple.wotaskd.plist
	sudo mv com.apple.wotaskd.plist /System/Library/LaunchDaemons
	touch /var/log/webobjects.log
	chown appserver /var/log/webobjects.log
	sudo launchctl load /System/Library/LaunchDaemons/com.apple.wotaskd.plist

`com.apple.wotaskd.plist` needs an ownership of `root/wheel` otherwise `launchctl` will refused to load it, citing "Dubious ownership".

### Project Wonder (Optional)

	mkdir wonder53 && pushd wonder53
	curl -LO 'http://webobjects.mdimension.com/wonder/Wonder-latest-Frameworks-53.tar.gz'
	tar xfz Wonder-latest-Frameworks-53.tar.gz
	cp -R \
		ERExtensions.framework \
		ERJars.framework \
		ERPrototypes.framework \
		JavaWOExtensions.framework \
		WOOgnl.framework \
		/Library/Frameworks
	popd

### MySQL JDBC (Optional)

* Download `mysql-connector-java-*.zip` from <http://dev.mysql.com/downloads/connector/j>
* Move `mysql-connector-java-*-bin.jar` into `/Library/Java/Extensions`.

### JavaMail (Optional)

Download v1.3.3: JavaMail 1.4 doesn't support `SMTP AUTH DIGEST-MD5`.

* Download: <http://java.sun.com/products/javamail/javamail-1_3_3.html>
* Move `mail.jar` into `/Library/Java/Extensions`.
* Download: <http://java.sun.com/javase/technologies/desktop/javabeans/jaf/downloads/index.html#download>
* Move `activation.jar` into `/Library/Java/Extensions`.

### JUnit (Optional)

* <http://sourceforge.net/projects/junit/files/junit/>
* Move `junit-*.jar` into `/Library/WebObjects/Extensions`.

## Development

### Eclipse 3.4.2

Eclipse 3.4.2 is the latest stable IDE.

Download *Eclipse IDE for Java Developers* at <http://www.eclipse.org/downloads/packages/release/ganymede/sr2>.

### WOLips

* Help > Software Updates > Available Software > Add Site... > http://webobjects.mdimension.com/wolips/stable
* Standard Install > everything except WOLips Goodies Win
* Install...

### Command-line Development (ant)

You need `woproject.jar` where `ant` can find it. Fortunately WOLips comes with its own copy of `woproject.jar`, so we can just set-up a symlink:

	sudo ln -s \
		~/Applications/eclipse/plugins/org.objectstyle.wolips.woproject.ant_3.4.*/lib/woproject.jar \
		/usr/share/ant/lib

NOTE: When you upgrade your WOLips, you'll probably have to re-issue this `ln` command to get command-line builds working again.

An alternative is to simply copy the jar file instead of linking it, but a broken symlink results in an somewhat-obvious error while I fear a significantly mismatched woproject from what I'm using in Eclipse+WOLips.




