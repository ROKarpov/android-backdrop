package io.github.rokarpov.backdrop

import android.os.Parcel
import android.os.Parcelable
import android.view.View

inline fun <reified T> parcelableCreator(
        crossinline ctor: (Parcel) -> T
) = object : Parcelable.Creator<T> {
    override fun createFromParcel(source: Parcel) = ctor(source)
    override fun newArray(size: Int) = arrayOfNulls<T>(size)
}

inline fun <reified T> parcelableClassLoaderCreator(
        crossinline ctor: (Parcel) -> T,
        crossinline ctor24: (Parcel, ClassLoader) -> T
) = object : Parcelable.ClassLoaderCreator<T> {
    override fun createFromParcel(source: Parcel) = ctor(source)
    override fun createFromParcel(source: Parcel, loader: ClassLoader) = ctor24(source, loader)
    override fun newArray(size: Int) = arrayOfNulls<T>(size)
}

