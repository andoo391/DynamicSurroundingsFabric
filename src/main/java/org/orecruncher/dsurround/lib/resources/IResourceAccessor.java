package org.orecruncher.dsurround.lib.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import org.orecruncher.dsurround.lib.validation.Validators;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * A resource accessor is used to obtain the content of a resource from within the JAR or from an external disk source.
 */
@Environment(EnvType.CLIENT)
public interface IResourceAccessor {

    /**
     * The resource location for the accessor
     *
     * @return Resource location
     */
    Identifier location();

    /**
     * Obtains the content of the resource as a string
     *
     * @return The resource data as a string, or null if not found
     */
    default String asString() {
        byte[] bytes = this.asBytes();
        return bytes != null ? new String(bytes, Charset.defaultCharset()) : null;
    }

    /**
     * Obtains the content of the resource as a series of bytes
     *
     * @return The resource data as an array of bytes, or null if not found
     */
    byte[] asBytes();

    /**
     * Obtains the content of the resource as a deserialized object
     *
     * @param clazz Class of the object to deserialize
     * @param <T>   The type of object that is being deserialized
     * @return Reference to the deserialized object, null if not possible
     */
    default <T> T as(final Class<T> clazz) {
        String content = this.asString();
        if (content != null) {
            try {
                final Gson gson = new GsonBuilder().create();
                final T obj = gson.fromJson(content, clazz);
                Validators.validate(obj);
                return obj;
            } catch (final Throwable t) {
                ResourceUtils.LOGGER.error(t, "Unable to complete processing of %s", this.toString());
            }
        }
        return null;
    }

    /**
     * Determines if the resource exists
     *
     * @return true if it exists, false otherwise
     */
    default boolean exists() {
        return asBytes() != null;
    }

    /**
     * Obtains the content of the resource as a deserialized object of the type specified.
     *
     * @param type Type of object instance to deserialize
     * @param <T>  The object type for casting
     * @return Reference to the deserialized object, null if not possible
     */
    default <T> T as(final Type type) {
        String content = this.asString();
        if (content != null) {
            try {
                final Gson gson = new GsonBuilder().create();
                final T obj = gson.fromJson(content, type);
                Validators.validate(obj, type);
                return obj;
            } catch (final Throwable t) {
                ResourceUtils.LOGGER.error(t, "Unable to complete processing of %s", this.toString());
            }
        }
        return null;
    }

    default void logError(final Throwable t) {
        if (t instanceof FileNotFoundException)
            ResourceUtils.LOGGER.debug("Asset not found for %s", this.toString());
        else
            ResourceUtils.LOGGER.error(t, "Unable to process asset %s", this.toString());
    }

    /**
     * Creates a reference to a resource accessor that can be used to retrieve data embedded in the JAR.
     *
     * @param rootContainer The location within the asset folder where data can be found.  Typically it's the mod ID.
     * @param location      The resource location of the data that needs to be retrieved.
     * @return Reference to a resource accessor to obtain the necessary data.
     */
    static IResourceAccessor createJarResource(final String rootContainer, final Identifier location) {
        return new ResourceAccessorJar(rootContainer, location);
    }

    /**
     * Creates a reference to a resource accessor that can be used to retrieve data on the local disk.
     *
     * @param root     The location on disk where the data can be found.
     * @param location The resource location of the data that needs to be retrieved.
     * @return Reference to a resource accessor to obtain the necessary data.
     */
    static IResourceAccessor createExternalResource(final File root, final Identifier location) {
        return new ResourceAccessorExternal(root, location);
    }

    /**
     * Creates a reference to a resource accessor that can be used to retrieve data from a resource pack.
     *
     * @param pack     Resource pack containing the needed resource data
     * @param location Location of the resource within the pack
     * @return Reference to a resource accessor to obtain the necessary data.
     */
    static IResourceAccessor createPackResource(final ResourcePack pack, final Identifier location, final Identifier actual) {
        return new ResourceAccessorPack(location, pack, actual);
    }

    /**
     * Iterates over a collection of accessors invoking an operation.  The operation is logged and encapsulated within
     * an error handling for logging purposes.  Exceptions will be suppressed.
     *
     * @param accessors Collection of accessors to invoke
     * @param consumer  The routine to invoke on each accessor.
     */
    static void process(final Collection<IResourceAccessor> accessors, final Consumer<IResourceAccessor> consumer) {
        for (final IResourceAccessor accessor : accessors) {
            ResourceUtils.LOGGER.info("Processing %s", accessor);
            try {
                consumer.accept(accessor);
            } catch (final Throwable t) {
                ResourceUtils.LOGGER.error(t, "Unable to complete processing of %s", accessor);
            }
        }
    }
}