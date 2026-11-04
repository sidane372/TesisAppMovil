package com.example.santaceciliatesis.util;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class MoneyUtilsTest {
    @Test public void preservesCentPrecision() { assertEquals("S/ 123456789.01", MoneyUtils.formatPen(12345678901L)); }
    @Test public void includesTwoDecimalPlaces() { assertEquals("S/ 1.00", MoneyUtils.formatPen(100)); }
    @Test public void handlesOneCent() { assertEquals("S/ 0.01", MoneyUtils.formatPen(1)); }
}
