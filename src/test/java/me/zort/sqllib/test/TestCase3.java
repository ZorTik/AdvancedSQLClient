package me.zort.sqllib.test;

import lombok.extern.log4j.Log4j2;
import me.zort.sqllib.naming.SymbolSeparatedNamingStrategy;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@EnabledOnOs(value = {OS.LINUX, OS.WINDOWS})
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCase3 { // Smaller independent modules tests

    @Test
    public void testSymbolSeparatedStrategy() {
        SymbolSeparatedNamingStrategy strategy = new SymbolSeparatedNamingStrategy('_');
        assertEquals("test", strategy.fieldNameToColumn("test"));
        assertEquals("test_two", strategy.fieldNameToColumn("testTwo"));
        assertEquals("this_is_third_test", strategy.fieldNameToColumn("thisIsThirdTest"));
    }

}
