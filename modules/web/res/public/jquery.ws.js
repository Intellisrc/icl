/**
 * A simple JQuery class to use web Sockets
 * @author A. Lepe
 * @since Aug 2nd, 2011
 */
$(function() {
    jQuery.WS = function(options, onmessage) {
        var ws = this;
        if ( !(ws instanceof jQuery.WS) )  {
            ws = new jQuery.WS(options, onmessage);
            return ws;
        } else {
            this.options = $.extend({ //DEFAULT
            url    : "localhost:8888", //URL to get the info from
            secure : false,
		    format : "json", //or json 
            log    : function(msg, status) {},
            on_open : function() {},
            on_close: function() {},
            msg    : onmessage
            }, options || {});
            if(options.auto_start) {
                this.start();
            }
        }
    }
    jQuery.WS.prototype = {
        socket : null,
        start : function() {
            var options = this.options;
            try{
                this.socket = new WebSocket((options.secure ? "wss" : "ws")+"://"+options.url);
                this.socket.onopen    = function(msg){ options.log("Connected",this.readyState); options.on_open(); };
                this.socket.onmessage = function(msg){ options.msg(msg.data); options.log("< "+msg.data);  };
                this.socket.onclose   = function(msg){ options.log("Disconnected",this.readyState); options.on_close(); };
            }
            catch(ex){ options.log("Error: "+ex); }
        },
        send : function(msg) {
            var options = this.options;
            if(!msg){ options.log("Message can not be empty"); return; }
            try{ 
                this.socket.send(msg); 
                options.log('> '+msg); 
            } 
            catch(ex){ options.log("Error: "+ex); }
        },
        quit: function() {
            this.socket.send("quit");
            try {
                this.socket.close();
            } catch(ignore) {}
            this.socket=null;
        }
    }
});
