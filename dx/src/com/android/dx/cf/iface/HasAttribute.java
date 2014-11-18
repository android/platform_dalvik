package com.android.dx.cf.iface;

/**
 * An element that can have {@link Attribute}
 */
public interface HasAttribute {

    /**
     * Get the element {@code attributes} (along with
     * {@code attributes_count}).
     *
     * @return {@code non-null;} the attributes list
     */
    public AttributeList getAttributes();

}
