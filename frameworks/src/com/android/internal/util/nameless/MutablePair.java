/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util.nameless;

import android.annotation.Nullable;

import java.util.Objects;

/**
 * Same as android.util.Pair but first and second is mutable.
 * @hide
 */
public class MutablePair<F, S> {

    public F first;
    public S second;

    public MutablePair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof MutablePair)) {
            return false;
        }
        MutablePair<?, ?> p = (MutablePair<?, ?>) o;
        return Objects.equals(p.first, first) && Objects.equals(p.second, second);
    }

    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
    }

    @Override
    public String toString() {
        return "MutablePair{" + String.valueOf(first) + " " + String.valueOf(second) + "}";
    }

    public void set(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <A, B> MutablePair <A, B> create(A a, B b) {
        return new MutablePair<A, B>(a, b);
    }
}
