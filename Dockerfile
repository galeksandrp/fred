FROM debian
COPY freenet_*.deb /root
RUN apt-get update && apt-get install -y \
  /root/freenet_*.deb \
  && rm -rf /var/lib/apt/lists/*

RUN chown -R freenet:freenet /etc/freenet

RUN ((sleep 30 && /etc/init.d/freenet stop) &) && /etc/init.d/freenet console

RUN wget -O /var/lib/freenet/plugins/UPnP.jar https://github.com/galeksandrp/plugin-UPnP/releases/download/10007/UPnP.jar

RUN sed -i -e 's/^fproxy.bindTo=.*/fproxy.bindTo=0.0.0.0/' \
  -e 's/^fproxy.allowedHosts=.*/fproxy.allowedHosts=*/' \
  -e 's/^fproxy.allowedHostsFullAccess=.*/fproxy.allowedHostsFullAccess=*/' \
  -e 's/^pluginmanager.loadplugin=.*/pluginmanager.loadplugin=UPnP/' /etc/freenet/freenet.ini

RUN ((sleep 30 && /etc/init.d/freenet stop) &) && /etc/init.d/freenet console

CMD ["/etc/init.d/freenet", "console"]
