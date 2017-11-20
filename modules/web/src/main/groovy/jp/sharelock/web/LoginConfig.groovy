package jp.sharelock.web

import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config as ConfigPac
import org.pac4j.core.config.ConfigFactory

import jp.sharelock.etc.Config
import org.pac4j.http.client.direct.DirectBasicAuthClient
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.http.client.indirect.IndirectBasicAuthClient
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator

/**
 * @since 17/11/20.
 */
class LoginConfig implements ConfigFactory {

    @Override
    ConfigPac build(Object... parameters) {
        final FormClient formClient = new FormClient("", new SimpleTestUsernamePasswordAuthenticator())
        final IndirectBasicAuthClient indirectBasicAuthClient = new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator())
        final DirectBasicAuthClient directBasicAuthClient = new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

        Config.get("")
        final clients = new Clients("")
        final config = new ConfigPac(clients)
        config.addAuthorizer()
        return config
    }
}
