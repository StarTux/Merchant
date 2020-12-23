package com.cavetale.merchant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Supplier;

public final class Json {
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    private Json() { }

    public static <T> T load(final File file, final Class<T> clazz, final Supplier<T> dfl) {
        if (!file.exists()) {
            return dfl.get();
        }
        try (FileReader fr = new FileReader(file)) {
            return GSON.fromJson(fr, clazz);
        } catch (FileNotFoundException fnfr) {
            return dfl.get();
        } catch (IOException ioe) {
            throw new IllegalStateException("Loading " + file, ioe);
        }
    }

    public static <T> T load(final File file, final Class<T> clazz) {
        return load(file, clazz, () -> null);
    }

    public static void save(final File file, final Object obj, final boolean pretty) {
        try (FileWriter fw = new FileWriter(file)) {
            Gson gs = pretty ? PRETTY_GSON : GSON;
            gs.toJson(obj, fw);
        } catch (IOException ioe) {
            throw new IllegalStateException("Saving " + file, ioe);
        }
    }
}
