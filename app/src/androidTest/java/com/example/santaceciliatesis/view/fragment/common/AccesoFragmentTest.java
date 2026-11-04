package com.example.santaceciliatesis.view.fragment.common;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.*;

import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.SantaCeciliaApplication;
import com.example.santaceciliatesis.view.activity.MainActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class AccesoFragmentTest {
    @Test
    public void registrationSuccessShowsNoticeAfterRegistrationFragmentIsDetached()
            throws Exception {
        SantaCeciliaApplication app = ApplicationProvider.getApplicationContext();
        app.getContainer().getSessionManager().awaitReady();
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        FragmentManager fragments = activity.getSupportFragmentManager();
                        activity.mostrarAcceso("LOGIN");
                        fragments.executePendingTransactions();
                        activity.mostrarAcceso("REGISTRO");
                        fragments.executePendingTransactions();
                        AccesoFragment registration =
                                (AccesoFragment)
                                        fragments.findFragmentById(R.id.fragment_container);
                        assertTrue(registration.isAdded());
                        assertEquals(1, fragments.getBackStackEntryCount());

                        // Reproduce la respuesta exitosa sin crear cuentas ni modificar sesiones.
                        registration.onRegistered();

                        assertFalse(registration.isAdded());
                        fragments.executePendingTransactions();
                        assertEquals(0, fragments.getBackStackEntryCount());
                        assertNotSame(
                                registration, fragments.findFragmentById(R.id.fragment_container));
                    });
            onView(withText(R.string.cuenta_creada)).check(matches(isDisplayed()));
        }
    }
}
