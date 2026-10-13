package com.shootspot.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.shootspot.entity.Location;
import com.shootspot.repository.LocationRepository;

@Component
public class SampleLocationSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleLocationSeeder.class);
    private static final String CHARMINAR_IMAGE =
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/71/Charminar_Hyderabad_1.jpg/1280px-Charminar_Hyderabad_1.jpg";
    private static final String HAMPI_TANK_IMAGE =
            "https://commons.wikimedia.org/wiki/Special:FilePath/Dancing_Girls_Bath_%28Hampi_water_tank%29%2C_Hampi%2C_Vijayanagara%2C_Karnataka%2C_India_%282012%29.jpg?width=1280";
    private static final String BENGALURU_PARK_IMAGE =
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7d/Band-Stand-Cubbon-Park-Bengaluru-karnataka-India.jpg/1280px-Band-Stand-Cubbon-Park-Bengaluru-karnataka-India.jpg";
    private static final String ANJUNA_BEACH_IMAGE =
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cf/Anjuna_Beach%2C_Goa%2C_India%2C_Legendary_Curlies_beach_shack.jpg/1280px-Anjuna_Beach%2C_Goa%2C_India%2C_Legendary_Curlies_beach_shack.jpg";
    private static final String MANDREM_BEACH_IMAGE =
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b3/Mandrem_Beach_and_Mandrem_River%2C_Mandrem%2C_Goa%2C_India.jpg/1280px-Mandrem_Beach_and_Mandrem_River%2C_Mandrem%2C_Goa%2C_India.jpg";
    private static final String KERALA_HOUSEBOAT_NIGHT_IMAGE =
            "https://upload.wikimedia.org/wikipedia/commons/0/0a/House_Boat_at_night%2C_Alappuzha%2C_Kerala.jpg";
    private static final String KERALA_HOUSEBOATS_IMAGE =
            "https://upload.wikimedia.org/wikipedia/commons/5/59/Kerala_house_boats.jpg";
    private static final String JAIPUR_HAWA_MAHAL_IMAGE =
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7a/Jaipur%2C_India%2C_Hawa_Mahal_Palace.jpg/1280px-Jaipur%2C_India%2C_Hawa_Mahal_Palace.jpg";

    private static final List<DemoLocationRevision> DEMO_LOCATION_REVISIONS = List.of(
            revision(1L, "Demo Jaipur Indoor Set", "Demo area, Jaipur", "Jaipur, Rajasthan",
                    "DEMO ONLY: Illustrative indoor-set listing, not a verified Jaipur business or confirmed available venue. The image shows a generic staged living room, not a Jaipur property.",
                    "https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=1200&q=80",
                    "Jaipur Daylight Apartment Set", "Jaipur, Rajasthan (sample area)",
                    "Jaipur, Rajasthan",
                    "Illustrative Jaipur apartment-set concept with a generic staged living-room photo. Confirm venue access and filming permission before booking.",
                    "https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=1200&q=80"),
            revision(2L, "Demo Anjuna Coastline Reference", "Anjuna area, Goa", "Goa",
                    "DEMO ONLY: Geographic visual reference, not a verified business or available beach venue. The photo depicts Anjuna Beach in Goa; it does not grant access or filming permission.",
                    ANJUNA_BEACH_IMAGE,
                    "Anjuna Beach", "Anjuna, Goa", "Goa",
                    "Anjuna Beach in Goa. Confirm beach access and filming permission before booking.",
                    ANJUNA_BEACH_IMAGE),
            revision(3L, "Demo Hampi Heritage Reference", "Hampi heritage area, Hampi", "Hampi, Karnataka",
                    "DEMO ONLY: Geographic visual reference, not a verified business or bookable site. The photo depicts the historic Dancing Girls Bath at Hampi; it does not grant entry or filming permission.",
                    HAMPI_TANK_IMAGE,
                    "Dancing Girls' Bath, Hampi", "Dancing Girls' Bath, Hampi, Karnataka",
                    "Hampi, Karnataka",
                    "Photo reference for the historic Dancing Girls' Bath at Hampi. Confirm site entry and filming permission before booking.",
                    HAMPI_TANK_IMAGE),
            revision(4L, "Jubilee Hills Sunlit Loft", "Road No. 36, Jubilee Hills", "Hyderabad",
                    "DEMO ONLY: Example of a Hyderabad-style daylight loft set, not a verified business or confirmed available venue. The photo is a generic staged interior and does not identify this address.",
                    "https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=1200&q=80",
                    "Jubilee Hills Daylight Living Room Set", "Jubilee Hills, Hyderabad (sample area)",
                    "Hyderabad",
                    "Illustrative daylight living-room set concept for Jubilee Hills; the photo shows a generic staged interior. Confirm access and filming permission before booking.",
                    "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=1200&q=80"),
            revision(5L, "Banjara Hills Heritage Courtyard", "Road No. 12, Banjara Hills", "Hyderabad",
                    "DEMO ONLY: Example of a neutral, daylight-filled indoor set for portraits, not a verified business or confirmed available venue. The photo shows a generic contemporary room, not a Hyderabad heritage courtyard.",
                    "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=1200&q=80",
                    "Banjara Hills Contemporary Lounge Set", "Banjara Hills, Hyderabad (sample area)",
                    "Hyderabad",
                    "Illustrative contemporary lounge concept for Banjara Hills with a generic staged interior photo. Confirm access and filming permission before booking.",
                    "https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?auto=format&fit=crop&w=1200&q=80"),
            revision(6L, "Film Nagar Creator Studio", "Film Nagar, Jubilee Hills", "Hyderabad",
                    "DEMO ONLY: Example of a compact, bright workspace set for small productions, not a verified business or confirmed available venue. The photo is an illustrative office interior, not this Hyderabad address.",
                    "https://images.unsplash.com/photo-1524758631624-e2822e304c36?auto=format&fit=crop&w=1200&q=80",
                    "Film Nagar Living Room Production Set", "Film Nagar, Jubilee Hills, Hyderabad (sample area)",
                    "Hyderabad",
                    "Illustrative living-room production set concept for Film Nagar with a generic interior photo. Confirm access and filming permission before booking.",
                    "https://images.unsplash.com/photo-1524758631624-e2822e304c36?auto=format&fit=crop&w=1200&q=80"),
            revision(7L, "Madhapur Minimal White Studio", "Ayyappa Society, Madhapur", "Hyderabad",
                    "DEMO ONLY: Example of a bright, neutral indoor shooting set, not a verified business or confirmed available venue. The photo is a generic staged interior and does not identify this Hyderabad address.",
                    "https://images.unsplash.com/photo-1600566753086-00f18fb6b3ea?auto=format&fit=crop&w=1200&q=80",
                    "Madhapur White Living Room Set", "Madhapur, Hyderabad (sample area)", "Hyderabad",
                    "Illustrative bright living-room concept for Madhapur with a generic residential-set photo. Confirm access and filming permission before booking.",
                    "https://images.unsplash.com/photo-1600566753086-00f18fb6b3ea?auto=format&fit=crop&w=1200&q=80"),
            revision(8L, "Gachibowli Glasshouse Lounge", "Indira Nagar, Gachibowli", "Hyderabad",
                    "DEMO ONLY: Example of a contemporary lounge set for brand shoots, not a verified business or confirmed available venue. The photo is a generic interior and does not identify this Hyderabad address.",
                    "https://images.unsplash.com/photo-1600607687644-c7171b42498f?auto=format&fit=crop&w=1200&q=80",
                    "Gachibowli Glass-Walled Meeting Set", "Gachibowli, Hyderabad (sample area)",
                    "Hyderabad",
                    "Illustrative glass-walled meeting-room concept for Gachibowli with a generic office photo. Confirm access and filming permission before booking.",
                    "https://images.unsplash.com/photo-1600607687644-c7171b42498f?auto=format&fit=crop&w=1200&q=80"),
            revision(9L, "Kondapur Boho Apartment Set", "Botanical Garden Road, Kondapur", "Hyderabad",
                    "DEMO ONLY: Example of a bohemian-style apartment interior for short-form video, not a verified business or confirmed available venue. The photo is illustrative and does not identify this Hyderabad address.",
                    "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&w=1200&q=80",
                    "Kondapur Bohemian Living Room Set", "Kondapur, Hyderabad (sample area)", "Hyderabad",
                    "Illustrative bohemian living-room concept for Kondapur with a generic staged interior photo. Confirm access and filming permission before booking.",
                    "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&w=1200&q=80"),
            revision(10L, "Kokapet Open-Sky Terrace", "Golden Mile Road, Kokapet", "Hyderabad",
                    "Open terrace with skyline views, simple seating and golden-hour light for creator reels, interviews and small campaigns.",
                    "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80",
                    "Kokapet Poolside Villa Set", "Kokapet, Hyderabad (sample area)", "Hyderabad",
                    "Illustrative poolside-home concept for Kokapet with a generic residence photo. Confirm access and filming permission before booking.",
                    "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=1200&q=80"),
            revision(11L, "Hitech City Editorial Workspace", "Cyber Towers Road, Hitech City", "Hyderabad",
                    "DEMO ONLY: Example of a contemporary office interior for corporate portraits and product stories, not a verified business or confirmed available venue. The image does not identify this Hyderabad address.",
                    "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=1200&q=80",
                    "HITEC City Open-Plan Office Set", "HITEC City, Hyderabad (sample area)", "Hyderabad",
                    "Illustrative open-plan office concept for corporate shoots in HITEC City. Confirm access and filming permission before booking.",
                    "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=1200&q=80"),
            revision(12L, "Sainikpuri Daylight Meeting Studio", "Vayupuri, Sainikpuri", "Hyderabad",
                    "DEMO ONLY: Example of a daylight-filled office meeting room used as an indoor shoot set, not a verified business or confirmed available venue. The photo is illustrative and does not identify this Hyderabad address.",
                    "https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=1200&q=80",
                    "Sainikpuri Conference Room Set", "Sainikpuri, Hyderabad (sample area)", "Hyderabad",
                    "Illustrative daylight conference-room concept for Sainikpuri with a generic office photo. Confirm access and filming permission before booking.",
                    "https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=1200&q=80"),
            revision(13L, "Hyderabad Charminar Visual Reference", "Old City, Hyderabad", "Hyderabad",
                    "DEMO ONLY: Geographic visual reference, not a verified business or bookable venue. The photo depicts Charminar, a public Hyderabad landmark; it does not grant access or filming permission.",
                    CHARMINAR_IMAGE,
                    "Charminar", "Charminar, Old City, Hyderabad", "Hyderabad",
                    "Charminar landmark photo reference in Hyderabad. Confirm public access and filming permission before booking.",
                    CHARMINAR_IMAGE),
            revision(14L, "Bengaluru Cubbon Park Visual Reference", "Cubbon Park area, Bengaluru", "Bengaluru, Karnataka",
                    "DEMO ONLY: Geographic visual reference, not a verified business or bookable park venue. The photo depicts the bandstand at Cubbon Park in Bengaluru; it does not confirm access or filming permission.",
                    BENGALURU_PARK_IMAGE,
                    "Cubbon Park Bandstand", "Cubbon Park, Bengaluru, Karnataka", "Bengaluru, Karnataka",
                    "Cubbon Park Bandstand photo reference in Bengaluru. Confirm park access and filming permission before booking.",
                    BENGALURU_PARK_IMAGE),
            revision(15L, "Demo Mandrem Beach Reference", "Mandrem area, Goa", "Goa",
                    "DEMO ONLY: Geographic visual reference, not a verified business or bookable beach venue. The photo depicts Mandrem Beach and river in Goa; it does not grant access or filming permission.",
                    MANDREM_BEACH_IMAGE,
                    "Mandrem Beach and River", "Mandrem, Goa", "Goa",
                    "Mandrem Beach and river in Goa. Confirm beach access and filming permission before booking.",
                    MANDREM_BEACH_IMAGE),
            revision(16L, "Demo Alappuzha Houseboat Reference", "Alappuzha backwater area", "Alappuzha, Kerala",
                    "DEMO ONLY: Geographic visual reference, not a verified business or available boat rental. The photo depicts a houseboat in Alappuzha, Kerala; it does not confirm operator, access or booking availability.",
                    KERALA_HOUSEBOAT_NIGHT_IMAGE,
                    "Alappuzha Houseboat at Night", "Alappuzha backwaters, Kerala", "Alappuzha, Kerala",
                    "Alappuzha houseboat photo reference showing a boat at night. Confirm operator, access, and filming permission before booking.",
                    KERALA_HOUSEBOAT_NIGHT_IMAGE),
            revision(17L, "Demo Kerala Backwater Houseboats Reference", "Alappuzha backwater area", "Alappuzha, Kerala",
                    "DEMO ONLY: Geographic visual reference, not a verified business or available boat rental. The photo depicts Kerala houseboats; it does not confirm an operator or booking availability.",
                    KERALA_HOUSEBOATS_IMAGE,
                    "Alappuzha Backwater Houseboats", "Alappuzha backwaters, Kerala", "Alappuzha, Kerala",
                    "Alappuzha backwater houseboat photo reference. Confirm operator, access, and filming permission before booking.",
                    KERALA_HOUSEBOATS_IMAGE),
            revision(18L, "Demo Jaipur Hawa Mahal Visual Reference", "Hawa Mahal area, Jaipur", "Jaipur, Rajasthan",
                    "DEMO ONLY: Geographic visual reference, not a verified business or bookable property. The photo depicts Jaipur's Hawa Mahal, a public landmark; it does not grant access or filming permission.",
                    JAIPUR_HAWA_MAHAL_IMAGE,
                    "Hawa Mahal", "Hawa Mahal, Jaipur, Rajasthan", "Jaipur, Rajasthan",
                    "Hawa Mahal landmark photo reference in Jaipur. Confirm public access and filming permission before booking.",
                    JAIPUR_HAWA_MAHAL_IMAGE));

    private final LocationRepository locationRepository;

    public SampleLocationSeeder(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Override
    public void run(String... args) {
        Map<Long, Location> existingById = new HashMap<>();
        locationRepository.findAll().forEach(location ->
                existingById.put(location.getId(), location));

        List<Location> revisedLocations = new ArrayList<>();
        for (DemoLocationRevision revision : DEMO_LOCATION_REVISIONS) {
            Location location = existingById.get(revision.id());
            if (location != null && applyRevision(location, revision)) {
                revisedLocations.add(location);
            }
        }

        if (!revisedLocations.isEmpty()) {
            locationRepository.saveAll(revisedLocations);
        }
        LOGGER.info("Demo location migration complete: updated {} existing records.",
                revisedLocations.size());
    }

    private static boolean applyRevision(Location location, DemoLocationRevision revision) {
        boolean recognizedSample = Objects.equals(location.getName(), revision.previousName())
                || Objects.equals(location.getAddress(), revision.previousAddress())
                || Objects.equals(location.getCity(), revision.previousCity());
        if (!recognizedSample) {
            return false;
        }

        boolean changed = false;
        changed |= replaceIfPrevious(
                location.getName(), revision.previousName(), revision.name(), location::setName);
        changed |= replaceIfPrevious(
                location.getAddress(), revision.previousAddress(), revision.address(), location::setAddress);
        changed |= replaceIfPrevious(
                location.getCity(), revision.previousCity(), revision.city(), location::setCity);
        changed |= replaceIfPrevious(
                location.getDescription(),
                revision.previousDescription(),
                revision.description(),
                location::setDescription);
        changed |= replaceIfPrevious(
                location.getImageUrl(), revision.previousImageUrl(), revision.imageUrl(), location::setImageUrl);
        return changed;
    }

    private static boolean replaceIfPrevious(
            String current, String previous, String replacement, Consumer<String> setter) {
        if (Objects.equals(current, previous) && !Objects.equals(current, replacement)) {
            setter.accept(replacement);
            return true;
        }
        return false;
    }

    static List<DemoLocationRevision> demoLocationRevisions() {
        return DEMO_LOCATION_REVISIONS;
    }

    private static DemoLocationRevision revision(
            Long id,
            String previousName,
            String previousAddress,
            String previousCity,
            String previousDescription,
            String previousImageUrl,
            String name,
            String address,
            String city,
            String description,
            String imageUrl) {
        return new DemoLocationRevision(
                id,
                previousName,
                previousAddress,
                previousCity,
                previousDescription,
                previousImageUrl,
                name,
                address,
                city,
                description,
                imageUrl);
    }

    record DemoLocationRevision(
            Long id,
            String previousName,
            String previousAddress,
            String previousCity,
            String previousDescription,
            String previousImageUrl,
            String name,
            String address,
            String city,
            String description,
            String imageUrl) {
    }
}
