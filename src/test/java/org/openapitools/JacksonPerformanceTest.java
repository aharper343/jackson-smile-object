package org.openapitools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapitools.jackson.nullable.JsonNullableModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.model.Category;
import org.openapitools.model.Pet;
import org.openapitools.model.Tag;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

class JacksonPerformanceTest {

    private static final long NANOS_IN_A_SECOND = 1_000_000_000L;
    private static final boolean DEBUG = false;

    private static final boolean DEBUG_RECORD = false;

    private static final Random RANDOM = new Random(12384754124L);

    private static final Pet PET = createPet(0);
    private static final Pet BIG_PET = createPet(100);

    private static final int WARM_ITERATIONS = 100;

    private static final int RECORD_ITERATIONS = 10_000;

    private static final int GC_MODULUS = 100;

    @BeforeAll
    static void beforeAll() {
        System.out.printf("small pet=%s\nbig pet=%s\n", PET, BIG_PET);
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false })
    void testObjectSmile(final boolean useBigPet) throws Exception {
        testObjectSmile(false, useBigPet);
        testObjectSmile(true, useBigPet);
    }

    void testObjectSmile(final boolean userAfterBurner, final boolean useBigPet) throws Exception {
        final String nm = (userAfterBurner ? "AB" : "no-AB") + "/" + (useBigPet ? "B" : "S");
        final Pet pet = useBigPet ? BIG_PET : PET;
        System.out.printf("\n\nObjectMapper - SmileMapper : %s\n", nm);
        final int objectLen = testObjectMapper(userAfterBurner, nm, pet);
        final int smileLen = testSmileMapper(userAfterBurner, nm, pet);
        System.out.printf("%-8s ObjectMapper=%d SmileMapper=%d ratio=%.2f%%\n", nm, objectLen, smileLen, 100f * ((double)smileLen) / ((double)objectLen));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false })
    void testSmileObject(final boolean useBigPet) throws Exception {
        testSmileObject(false, useBigPet);
        testSmileObject(true, useBigPet);
    }

    void testSmileObject(final boolean userAfterBurner, final boolean useBigPet) throws Exception {
        final String nm = (userAfterBurner ? "AB" : "no-AB") + "/" + (useBigPet ? "B" : "S");
        final Pet pet = useBigPet ? BIG_PET : PET;
        System.out.printf("\n\nSmileMapper - ObjectMapper : %s\n", nm);
        final int smileLen = testSmileMapper(userAfterBurner, nm, pet);
        final int objectLen = testObjectMapper(userAfterBurner, nm, pet);
        System.out.printf("%-8s ObjectMapper=%d SmileMapper=%d ratio=%.2f%%\n", nm, objectLen, smileLen, 100f * ((double)smileLen) / ((double)objectLen));
    }

    int testSmileMapper(final boolean useAfterBurner, final String nm, final Pet pet) throws Exception {
        final SmileMapper smileMapper = new SmileMapper();
        configure(smileMapper, useAfterBurner);
        return test("SmileMapper-" + nm, smileMapper, pet);
    }

    int testObjectMapper(final boolean userAfterBurner, final String nm, final Pet pet) throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        configure(objectMapper, userAfterBurner);
        return test("ObjectMapper-" + nm, objectMapper, pet);
    }

    int test(final String name, final ObjectMapper objectMapper, final Pet pet) throws Exception {
        if (DEBUG) {
            System.out.printf("%s: %s\n", name, ToStringBuilder.reflectionToString(objectMapper, RecursiveToStringStyle.MULTI_LINE_STYLE));
        }
        test(name, objectMapper, WARM_ITERATIONS, false, pet);
        return test(name, objectMapper, RECORD_ITERATIONS, true, pet);
    }

    int test(final String name, final ObjectMapper objectMapper, final int iterations, final boolean record, final Pet pet) throws Exception {
        byte [] bytes = new byte[0];
        long durationNS = 0;
        for (int i = 0; i < iterations; i++) {
            if (i % GC_MODULUS == 0) {
                System.gc();
            }
            final long startNS = System.nanoTime();
            bytes = objectMapper.writeValueAsBytes(pet);
            durationNS += System.nanoTime() - startNS;
        }
        final Duration duration = Duration.of(durationNS, ChronoUnit.NANOS);
        if (DEBUG || DEBUG_RECORD || record) {
            final long avgOpDurationNS = durationNS / (long)iterations;
            final Duration avgOpDuration = Duration.of(avgOpDurationNS, ChronoUnit.NANOS);
            final long opsPerSecond = NANOS_IN_A_SECOND / avgOpDurationNS;
            System.out.printf("%-20s %-5s iterations=%-10d total duration=%-14s average operation duration=%-14s ops/s=%-10d\n", name, record, iterations, duration, avgOpDuration, opsPerSecond);
        }
        if (DEBUG) {
            final String result = (new String(bytes, StandardCharsets.US_ASCII)).replace('\n', '?');
            System.out.printf("%-20s %-5s size=%-8d -> %s\n", name, record, bytes.length, result);
        }
        return bytes.length;
    }

    private void configure(final ObjectMapper objectMapper, final boolean useAfterBurner) {
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        objectMapper.findAndRegisterModules();
        objectMapper.registerModule(new JsonNullableModule());
        objectMapper.registerModule(new JavaTimeModule());
        if (useAfterBurner) {
            objectMapper.registerModule(new com.fasterxml.jackson.module.afterburner.AfterburnerModule());
        }
    }

    static Pet createPet(final int numTags) {
        final Pet pet = new Pet();
        pet.setId(id());
        pet.name(name());
        pet.setStatus(JsonNullable.of(status()));
        pet.category(createCategory());
        pet.setTags(createTags(numTags));
        return pet;
    }

    static List<Tag> createTags(final int numTags) {
        final List<Tag> tags = new ArrayList<>(numTags);
        for (int i = 0; i < numTags; i++) {
            tags.add(createTag());
        }
        return tags;
    }

    static Tag createTag() {
        final Tag tag = new Tag();
        tag.id(id());
        tag.name(name());
        return tag;
    }

    static Category createCategory() {
        final Category category = new Category();
        category.id(id());
        category.name(UUID.randomUUID().toString());
        return category;
    }

    static Pet.StatusEnum status() {
        //final int value = Math.abs(RANDOM.nextInt()) % 3;
        final int value = 0;
        return switch (value) {
            case 0 -> Pet.StatusEnum.AVAILABLE;
            case 1 -> Pet.StatusEnum.PENDING;
            default -> Pet.StatusEnum.SOLD;
        };
    }


    static long id() {
        return Math.abs(RANDOM.nextLong());
    }

    static String name() {
        return new UUID(RANDOM.nextLong(), RANDOM.nextLong()).toString();
    }
}
