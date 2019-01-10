package com.intellisrc.core

import spock.lang.Specification


/**
 * @since 19/01/10.
 */
class ConfigTest extends Specification {
    def "Simple config file"() {
        setup:
            File testCfg = new File(SysInfo.tempDir, "test.config")
            Config.CfgFile cfg = new Config.CfgFile(testCfg)
            cfg.set("text","something")
            cfg.set("number", 10)
            cfg.set("double", 1.1d)
            cfg.set("istrue", true)
        expect:
            assert testCfg.exists()
            assert !testCfg.text.empty
            assert cfg.get("text") == "something"
            assert cfg.getInt("number") == 10
            assert cfg.getDbl("double") == 1.1d
            assert cfg.getBool("istrue")
            assert !cfg.getBool("isfalse")
        cleanup:
            testCfg.delete()
    }
    def "Test System Config" () {
        setup:
            Config.system.set("custom", 100)
        expect:
            assert Config.system.getInt("custom") == 100
            assert Config.system.get("java.home")
            println Config.system.get("java.home")
    }
}