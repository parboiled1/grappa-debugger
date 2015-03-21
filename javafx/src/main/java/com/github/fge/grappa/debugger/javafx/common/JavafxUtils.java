package com.github.fge.grappa.debugger.javafx.common;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.paint.Color;

import java.util.function.Function;

public final class JavafxUtils
{
    public static final Color FILL_COLOR_1
        = Color.rgb(78, 164, 78);
    public static final Color FILL_COLOR_2
        = Color.rgb(234, 160, 41);
    public static final Color FILL_COLOR_3
        = Color.rgb(224, 97, 49);
    public static final Color FILL_COLOR_4
        = Color.rgb(72, 164, 192);
    public static final String GRAY = "-fx-fill: GRAY;";
    public static final String GREEN_UNDERLINED = "-fx-fill: GREEN; -fx-underline: true;";
    public static final String RED_UNDERLINED = "-fx-fill: RED; -fx-underline: true;";


    @SuppressWarnings("ProhibitedExceptionThrown")
    private JavafxUtils()
    {
        throw new Error("nice try!");
    }

    @SuppressWarnings("AutoBoxing")
    public static String nanosToString(final long nanos)
    {
        long value = nanos;
        final long nrNanoseconds = value % 1000;
        value /= 1000;
        final long nrMicroseconds = value % 1000;
        value /= 1000;

        return String.format("%d ms, %03d.%03d µs", value, nrMicroseconds,
            nrNanoseconds);
    }

    public static <S, T> void setColumnValue(final TableColumn<S, T> column,
        final Function<? super S, ? extends T> f)
    {
        column.setCellValueFactory(
            param -> new SimpleObjectProperty<T>()
            {
                @Override
                public T get()
                {
                    return f.apply(param.getValue());
                }
            }
        );
    }
}
