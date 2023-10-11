package com.minecolonies.api.util;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 使用自定义哈希码和相等性实现的元组。
 *
 * @param <A> 第一个对象。
 * @param <B> 第二个对象。
 */
public class Tuple<A, B>
{
    private A a;
    private B b;

    public Tuple(@Nullable final A aIn, @Nullable final B bIn)
    {
        this.a = aIn;
        this.b = bIn;
    }

    @Nullable
    public A getA()
    {
        return this.a;
    }

    @Nullable
    public B getB()
    {
        return this.b;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(a, b);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final Tuple<?, ?> tuple = (Tuple<?, ?>) o;
        return Objects.equals(a, tuple.a) &&
                 Objects.equals(b, tuple.b);
    }
}
