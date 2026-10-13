package com.shootspot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.shootspot.entity.Location;
import com.shootspot.repository.LocationRepository;

class SampleLocationSeederTest {

    @Test
    void revisesExistingSamplesWithExpectedLocationsAndPreservesIdsAndUnrelatedFields() {
        LocationRepository repository = Mockito.mock(LocationRepository.class);
        List<Location> existing = currentSampleRecords();
        when(repository.findAll()).thenReturn(existing);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        new SampleLocationSeeder(repository).run();

        List<Location> saved = captureSavedLocations(repository);
        Map<Long, Location> savedById = saved.stream()
                .collect(Collectors.toMap(Location::getId, Function.identity()));
        assertEquals(18, savedById.size());
        assertEquals(18, existing.size());
        for (int index = 0; index < SampleLocationSeeder.demoLocationRevisions().size(); index++) {
            SampleLocationSeeder.DemoLocationRevision revision =
                    SampleLocationSeeder.demoLocationRevisions().get(index);
            Location location = savedById.get(revision.id());
            assertEquals(revision.name(), location.getName());
            assertEquals(revision.address(), location.getAddress());
            assertEquals(revision.city(), location.getCity());
            assertEquals(revision.description(), location.getDescription());
            assertEquals(revision.imageUrl(), location.getImageUrl());
            assertEquals("existing-category-" + revision.id(), location.getCategory());
            assertEquals(BigDecimal.valueOf(800 + revision.id()), location.getPricePerHour());
            assertEquals(revision.id() % 2 == 0, location.isAvailable());
        }

        assertEquals(10L, existing.stream().filter(location -> "Hyderabad".equals(location.getCity())).count());
        assertEquals(2L, existing.stream().filter(location -> "Goa".equals(location.getCity())).count());
        assertEquals(2L, existing.stream().filter(location -> "Alappuzha, Kerala".equals(location.getCity())).count());
        assertEquals(2L, existing.stream().filter(location -> "Jaipur, Rajasthan".equals(location.getCity())).count());
        assertEquals(2L, existing.stream().filter(location -> "Hampi, Karnataka".equals(location.getCity())
                || "Bengaluru, Karnataka".equals(location.getCity())).count());
    }

    @Test
    void migrationIsIdempotentAndPreservesLaterAdminEdits() {
        LocationRepository repository = Mockito.mock(LocationRepository.class);
        List<Location> existing = currentSampleRecords();
        when(repository.findAll()).thenAnswer(invocation -> List.copyOf(existing));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        SampleLocationSeeder seeder = new SampleLocationSeeder(repository);

        seeder.run();
        Location customized = existing.stream()
                .filter(location -> Long.valueOf(4L).equals(location.getId()))
                .findFirst()
                .orElseThrow();
        customized.setImageUrl("https://cdn.example.org/updated-admin-image.jpg");
        customized.setDescription("Updated by an administrator after migration.");
        seeder.run();

        verify(repository, times(1)).saveAll(anyList());
        assertEquals("https://cdn.example.org/updated-admin-image.jpg", customized.getImageUrl());
        assertEquals("Updated by an administrator after migration.", customized.getDescription());
    }

    @Test
    void updatesOnlyFieldsStillMatchingKnownSampleValues() {
        LocationRepository repository = Mockito.mock(LocationRepository.class);
        List<Location> existing = currentSampleRecords();
        Location customized = existing.stream()
                .filter(location -> Long.valueOf(4L).equals(location.getId()))
                .findFirst()
                .orElseThrow();
        customized.setName("My renamed studio");
        customized.setDescription("Admin-maintained description.");
        customized.setImageUrl("https://cdn.example.org/admin-custom.jpg");
        when(repository.findAll()).thenReturn(existing);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        new SampleLocationSeeder(repository).run();

        List<Location> saved = captureSavedLocations(repository);
        assertEquals("My renamed studio", customized.getName());
        assertEquals("Admin-maintained description.", customized.getDescription());
        assertEquals("https://cdn.example.org/admin-custom.jpg", customized.getImageUrl());
        assertEquals("Jubilee Hills, Hyderabad (sample area)", customized.getAddress());
        assertEquals("Hyderabad", customized.getCity());
        assertTrue(saved.stream().anyMatch(location -> Long.valueOf(4L).equals(location.getId())));
    }

    @Test
    void skipsRowsWhoseKnownSampleIdentityHasBeenFullyReplaced() {
        LocationRepository repository = Mockito.mock(LocationRepository.class);
        List<Location> existing = currentSampleRecords();
        Location customized = existing.stream()
                .filter(location -> Long.valueOf(4L).equals(location.getId()))
                .findFirst()
                .orElseThrow();
        customized.setName("Independent venue");
        customized.setAddress("Custom address");
        customized.setCity("Custom city");
        when(repository.findAll()).thenReturn(existing);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        new SampleLocationSeeder(repository).run();

        assertEquals("Independent venue", customized.getName());
        assertEquals("Custom address", customized.getAddress());
        assertEquals("Custom city", customized.getCity());
        List<Location> saved = captureSavedLocations(repository);
        assertFalse(saved.stream().anyMatch(location -> Long.valueOf(4L).equals(location.getId())));
    }

    @Test
    void allPersistedSeedValuesFitVarchar255AndDescriptionsExplainBookingConditions() {
        Set<String> imageUrls = new HashSet<>();
        for (SampleLocationSeeder.DemoLocationRevision revision : SampleLocationSeeder.demoLocationRevisions()) {
            assertAtMost255Characters(revision.name(), "name", revision.id());
            assertAtMost255Characters(revision.address(), "address", revision.id());
            assertAtMost255Characters(revision.city(), "city", revision.id());
            assertAtMost255Characters(revision.description(), "description", revision.id());
            assertAtMost255Characters(revision.imageUrl(), "imageUrl", revision.id());
            assertFalse(revision.description().startsWith("DEMO ONLY:"));
            assertTrue(revision.description().toLowerCase().contains("confirm"));
            assertFalse(revision.name().toLowerCase().contains("demo"));
            assertFalse(revision.name().toLowerCase().contains("reference"));
            assertTrue(imageUrls.add(revision.imageUrl()), "Duplicate seeded image URL: " + revision.imageUrl());
        }
    }

    private static void assertAtMost255Characters(String value, String field, Long locationId) {
        assertTrue(value.length() <= 255,
                () -> "Location " + locationId + " " + field + " exceeds 255 characters: " + value.length());
    }

    private static List<Location> currentSampleRecords() {
        return SampleLocationSeeder.demoLocationRevisions().stream()
                .map(SampleLocationSeederTest::currentRecord)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Location currentRecord(SampleLocationSeeder.DemoLocationRevision revision) {
        Location location = new Location();
        location.setId(revision.id());
        location.setName(revision.previousName());
        location.setAddress(revision.previousAddress());
        location.setCity(revision.previousCity());
        location.setCategory("existing-category-" + revision.id());
        location.setPricePerHour(BigDecimal.valueOf(800 + revision.id()));
        location.setDescription(revision.previousDescription());
        location.setImageUrl(revision.previousImageUrl());
        location.setAvailable(revision.id() % 2 == 0);
        return location;
    }

    private static List<Location> captureSavedLocations(LocationRepository repository) {
        org.mockito.ArgumentCaptor<List<Location>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }
}
