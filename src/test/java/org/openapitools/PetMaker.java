package org.openapitools;

import com.github.javafaker.Faker;
import org.openapitools.model.Category;
import org.openapitools.model.Pet;
import org.openapitools.model.Tag;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PetMaker {

    private final Random random;

    private final Faker faker;

    public PetMaker(final Random random) {
        this.random = random;
        this.faker = new Faker(random);
    }
    public Pet createPet(final int numTags) {
        final Pet pet = new Pet();
        pet.id(id());
        pet.name(faker.cat().name());
        pet.status(status());
        pet.category(createCategory());
        pet.createdOn(offsetDateTime());
        pet.tags(createTags(numTags));
        return pet;
    }

    public List<Tag> createTags(final int numTags) {
        final List<Tag> tags = new ArrayList<>(numTags);
        for (int i = 0; i < numTags; i++) {
            tags.add(createTag());
        }
        return tags;
    }

    public Tag createTag() {
        final Tag tag = new Tag();
        tag.name(faker.job().title());
        tag.id(id(tag.getName()));
        return tag;
    }

    public Category createCategory() {
        final Category category = new Category();
        category.name(faker.witcher().monster());
        category.id(id(category.getName()));
        return category;
    }

    public Pet.StatusEnum status() {
        final int value = Math.abs(random.nextInt()) % (Pet.StatusEnum.values().length - 1);
        return Pet.StatusEnum.values()[value];
    }

    private OffsetDateTime offsetDateTime() {
        return Instant.ofEpochMilli(time()).atOffset(ZoneOffset.UTC).withYear(2024);
    }

    private long time()  {
        return Math.abs(random.nextLong());
    }

    private long id() {
        return Math.abs((long)random.nextInt());
    }

    private long id(final String name) {
        return Math.abs(4969L * (long)name.hashCode() * (long)name.length());
    }
}
