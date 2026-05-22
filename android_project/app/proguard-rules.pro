# Add project specific ProGuard rules here.

-keep class io.github.gdict.** { *; }
-dontwarn io.github.gdict.**

-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn javax.xml.stream.**
-dontwarn org.codehaus.stax2.**
-dontwarn com.ctc.wstx.**
