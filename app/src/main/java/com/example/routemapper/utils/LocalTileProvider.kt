package com.example.routemapper.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class LocalTileProvider(private val context: Context) : TileProvider {

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        val assetManager = context.assets
        val tilePath = "tiles/$zoom/$x/$y.png"
        Log.e("TAG", "$zoom, $x, $y, $tilePath")
        print("$zoom, $x, $y, $tilePath")
        return try {
            val inputStream = assetManager.open(tilePath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val stream = ByteArrayOutputStream()
            Log.e("TAG", "bitmap: $bitmap")

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            Tile(TILE_SIZE, TILE_SIZE, stream.toByteArray())
        } catch (e: IOException) {

            TileProvider.NO_TILE
        }
    }

    companion object {
        private const val TILE_SIZE = 256
    }
}