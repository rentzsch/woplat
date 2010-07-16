# Recipe for Installing WebObjects 5.3 and 5.4 on Debian v5 Lenny

## Create a new VMware VM

* 1024 MB RAM (will reduce to 256MB once the auto-partitioner has run)
* 250 GB Hard Disk file (it's sparse, so give us room for log files and DB files)

## Install debian-500-i386-netinst.iso

* English
* United States
* American English
* Hostname: wovm
* Domain name: webobjects.com
* Time Zone: Central
* Guided - use entire disk
* SCSI1
* All files in one partition
* root password: pass (You'll want to change this later.)
* nonadmin account: wolf
* wolf password: pass
* Mirror country: United States
* Archive mirror: debian.uchicago.edu
* Empty HTTP Proxy
* Opt-in to package usage survey
* Disable all software (Desktop environment and Standard system)
* Install GRUB to the Master Boot Record
* Allow restart
* Shutdown, reduce RAM back to 256MB
* Start-up

## Login

User: root  
Password: pass

(You'll want to change this later.)

## Install ssh

	apt-get install ssh

*Now would be a good time to switch from typing directly into the VM's window to ssh-via-Terminal.app so you can leverage copy & pasting for commands. Use `ifconfig eth0` to discover your VM's IP address.*

## Remove nonadmin user

You can add your own users later.

	userdel --remove wolf

## Install Mercurial

	apt-get install mercurial
	cd /etc && hg init && hg add && hg ci -m 'initial commit'

## Add contrib and non-free apt sources (prep for installing Java)

	pico /etc/apt/sources.list

Add "contrib non-free" source qualifiers to the file:

<pre>
    # 
    # deb cdrom:[Debian GNU/Linux 5.0.0 _Lenny_ - Official i386 NETINST Binary-1 20090214-16:03]/ lenny main
 
    #deb cdrom:[Debian GNU/Linux 5.0.0 _Lenny_ - Official i386 NETINST Binary-1 20090214-16:03]/ lenny main
 
-   deb http://debian.uchicago.edu/debian/ lenny main
+   deb http://debian.uchicago.edu/debian/ lenny main contrib non-free
    deb-src http://debian.uchicago.edu/debian/ lenny main
 
-   deb http://security.debian.org/ lenny/updates main
+   deb http://security.debian.org/ lenny/updates main contrib non-free
    deb-src http://security.debian.org/ lenny/updates main
 
-   deb http://volatile.debian.org/debian-volatile lenny/volatile main
+   deb http://volatile.debian.org/debian-volatile lenny/volatile main contrib non-free
    deb-src http://volatile.debian.org/debian-volatile lenny/volatile main
</pre>

Save your changes, update the source:
	
	apt-get update
	cd /etc && hg addremove && hg ci -m 'Add contrib and non-free apt source qualifiers.'

## Install Java 5

	apt-get install sun-java5-jre
	<agree to prompts>
	cd /etc && hg addremove && hg ci -m 'apt-get install sun-java5-jre'

## Work-around [Sun Bug ID 6456628 "Default timezone is incorrectly set occasionally on Linux"](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6456628)

This work-around is critical, otherwise WebObjects apps won't start cause they can't parse timezones formatted like "`SystemV/CST6CDT`". `NSLog`'s constructor tries to construct a `NSTimezone`, which fails. `NSTimezone` tries to report its failure by constructing an `NSLog`, leading to an immediate silent abort of the entire process.

	mkdir /etc/sysconfig && echo 'ZONE="America/Chicago"' > /etc/sysconfig/clock
	cd /etc && hg addremove && hg ci -m 'set timezone in /etc/sysconfig/clock'

## Install WebObjects v5.3.3 and v5.4.3

	cd; wget http://webobjects.mdimension.com/wolips/WOInstaller.jar
	
	mkdir /opt/wo533 && mkdir /opt/wo543

	java -jar WOInstaller.jar 5.3.3 /opt/wo533
	java -jar WOInstaller.jar 5.4.3 /opt/wo543

	# Pick a version. We're defaulting to 5.3.3.
	ln -s /opt/wo533/Local /opt && ln -s /opt/wo533/Library /opt

	rm WOInstaller.jar

## Add appserver user and appserveradm group

	groupadd appserveradm
	useradd -g appserveradm --create-home appserver
	
	chown -R appserver:appserveradm /opt/wo533/Local
	chown -R appserver:appserveradm /opt/wo533/Library
	chmod 750 /opt/wo533/Library/WebObjects/JavaApplications/JavaMonitor.woa/JavaMonitor
	chmod 750 /opt/wo533/Library/WebObjects/JavaApplications/wotaskd.woa/Contents/Resources/SpawnOfWotaskd.sh
	chmod 750 /opt/wo533/Library/WebObjects/JavaApplications/wotaskd.woa/wotaskd
	
	chown -R appserver:appserveradm /opt/wo543/Local
	chown -R appserver:appserveradm /opt/wo543/Library
	chmod 750 /opt/wo543/Library/WebObjects/JavaApplications/JavaMonitor.woa/JavaMonitor
	chmod 750 /opt/wo543/Library/WebObjects/JavaApplications/wotaskd.woa/Contents/Resources/SpawnOfWotaskd.sh
	chmod 750 /opt/wo543/Library/WebObjects/JavaApplications/wotaskd.woa/wotaskd
	
	su - appserver
	echo 'export NEXT_ROOT=/opt' > .bash_profile
	mkdir /opt/Local/Library/WebObjects/Apps && mkdir /opt/Local/Library/WebObjects/Logs
	exit
	
	cd /etc && hg addremove && hg ci -m 'useradd appserver && groupadd appserveradm'

## Install apache

	apt-get install apache2
	cd /etc && hg addremove && hg ci -m 'apt-get install apache2'

## Install Adaptor (part 1 of 3)

	WOPLAT=woplat-20090423_debian-5_apache-2.2
	cd && wget http://cloud.github.com/downloads/rentzsch/woplat/$WOPLAT.tgz
	tar xfz $WOPLAT.tgz && rm $WOPLAT.tgz

## Either compile the web server adaptor yourself... (part 2 of 3)

	apt-get install git-core build-essential apache2-threaded-dev
	cd && git clone git://github.com/rentzsch/woplat.git
	cd woplat/Adaptors
	perl -pi -e 's/ADAPTOR_OS = MACOS/ADAPTOR_OS = LINUX/' make.config
	make
	apxs2 -i -a -n WebObjects Apache2.2/mod_WebObjects.la

## ... -or- just install the pre-compiled adaptor binary (part 2 of 3)

	mv ~/$WOPLAT/mod_WebObjects.so /usr/lib/apache2/modules/

## Install Adaptor (part 3 of 3)

	mv ~/$WOPLAT/apache_webobjects.conf /etc/apache2/apache_webobjects.conf
	pico /etc/apache2/sites-enabled/000-default

<pre>
+	Include /etc/apache2/apache_webobjects.conf
 
 	&lt;VirtualHost *:80>
 		ServerAdmin webmaster@localhost
 	
 		DocumentRoot /var/www/
 		&lt;Directory />
 			Options FollowSymLinks
 			AllowOverride None
 		&lt;/Directory>
 		&lt;Directory /var/www/>
 			Options Indexes FollowSymLinks MultiViews
 			AllowOverride None
 			Order allow,deny
 			allow from all
 		&lt;/Directory>
 	
-		ScriptAlias /cgi-bin/ /usr/lib/cgi-bin/
-		&lt;Directory "/usr/lib/cgi-bin">
-			AllowOverride None
-			Options +ExecCGI -MultiViews +SymLinksIfOwnerMatch
-			Order allow,deny
-			Allow from all
-		&lt;/Directory>
 
 		ErrorLog /var/log/apache2/error.log
 
 		# Possible values include: debug, info, notice, warn, error, crit,
 		# alert, emerg.
 		LogLevel warn
 
 		CustomLog /var/log/apache2/access.log combined
 
 		Alias /doc/ "/usr/share/doc/"
 		&lt;Directory "/usr/share/doc/">
 			Options Indexes MultiViews FollowSymLinks
 			AllowOverride None
 			Order deny,allow
 			Deny from all
 			Allow from 127.0.0.0/255.0.0.0 ::1/128
 		&lt;/Directory>
 
 	&lt;/VirtualHost>
</pre>

	apache2ctl configtest && apache2ctl restart
	
	cd /etc && hg addremove && hg ci -m 'Install apache mod_WebObjects'

## Install webobjects init.d

	cd ~/$WOPLAT
	chmod u+x webobjects.init.d
	mv ~/$WOPLAT/webobjects.init.d /etc/init.d/webobjects
	update-rc.d webobjects defaults
	cd /etc && hg addremove && hg ci -m 'Install WebObjects init.d'

	# Clean up woplat
	rm -rf ~/$WOPLAT

## Start WebObjects

	/etc/init.d/webobjects start

## Use JavaMonitor to Initialize Your Host

* Open http://192.168.22.xxx:56789/ in your browser
* Hosts tab > Add Host: "localhost" of type: Unix
* Preferences tab > Set a password. This is used by both wotaskd and JavaMonitor.

## Strengthen your root password

	passwd

## Install MySQL

	apt-get install mysql-server libmysql-java
	cd /etc && hg addremove && hg ci -m 'apt-get install mysql-server libmysql-java'
	# Put the .jar where WO will find it.
	ln -s /usr/share/java/mysql-connector-java.jar /opt/Local/Library/WebObjects/Extensions
		
	# for some reason mysql brings along a mail server. Turn it off.
	/etc/init.d/exim4 stop
	update-rc.d -f exim4 remove
	cd /etc && hg addremove && hg ci -m 'update-rc.d -f exim4 remove'

## (Optional) Install Wonder

	su - appserver
	mkdir /opt/Local/Library/Frameworks && cd /opt/Local/Library/Frameworks
	wget http://webobjects.mdimension.com/wonder/Wonder-latest-Frameworks-53.tar.gz
	tar xfz Wonder-latest-Frameworks-53.tar.gz && rm Wonder-latest-Frameworks-53.tar.gz

## (Optional) Install Mail Jars

If you have WebObejcts applications that send email, you'll probably need activation.jar and mail.jar.

	scp activation.jar mail.jar root@example.com:/opt/Local/Library/WebObjects/Extensions

## (Optional) Install lsof

	apt-get install lsof
	lsof -i -P|grep LISTEN|grep java

## (Optional) Install ntp

	apt-get install ntp
	cd /etc && hg addremove && hg ci -m 'apt-get install ntp'

## (Optional) Install munin

	apt-get install munin munin-node
	cd /etc && hg addremove && hg ci -m 'apt-get install munin munin-node'

*Reports will be at http://server/munin after a while -- the cron job needs to run first.*

## DHCP -> Static Address

*Best do this connected directly to the VMware window since any ssh connections will be terminated.*

	ifdown eth0
	pico /etc/network/interfaces
<pre>
 	# This file describes the network interfaces available on your system
 	# and how to activate them. For more information, see interfaces(5).
 
 	# The loopback network interface
 	auto lo
 	iface lo inet loopback
 
 	# The primary network interface
 	allow-hotplug eth0
-	iface eth0 inet dhcp
+	#iface eth0 inet dhcp
+	iface eth0 inet static
+		address 12.34.56.78
+		netmask 255.255.255.0
+		gateway 12.34.56.71
</pre>
	ifup eth0
	cd /etc && hg addremove && hg ci -m 'change to static IP address'