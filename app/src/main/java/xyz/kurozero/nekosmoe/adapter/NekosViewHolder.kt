package xyz.kurozero.nekosmoe.adapter

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import kotlinx.android.synthetic.main.grid_list_item.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import xyz.kurozero.nekosmoe.model.Neko
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.github.kittinunf.fuel.Fuel
import com.google.android.material.snackbar.Snackbar
import com.hendraanggrian.pikasso.into
import com.hendraanggrian.pikasso.picasso
import com.stfalcon.frescoimageviewer.ImageViewer
import kotlinx.android.synthetic.main.dialog_view_neko.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import xyz.kurozero.nekosmoe.*
import xyz.kurozero.nekosmoe.helper.Api
import xyz.kurozero.nekosmoe.helper.hasPermissions
import xyz.kurozero.nekosmoe.helper.isConnected
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

lateinit var file: File

class NekosViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindView(neko: Neko) {
        itemView.nekoImg.foreground = itemView.context.getDrawable(R.drawable.image_border)

        val radius = itemView.context.resources.getDimensionPixelSize(R.dimen.corner_radius)

        Glide.with(itemView.context)
            .load(neko.getThumbnailUrl())
            .transforms(CenterCrop(), RoundedCorners(radius))
            .into(itemView.nekoImg)

        itemView.nekoImg.onClick {
            val view = LayoutInflater.from(itemView.context).inflate(R.layout.dialog_view_neko, null)
            val nekoDialog = AlertDialog.Builder(itemView.context)
                .setView(view)
                .create()

            view.tvUploader.text = itemView.context.getString(R.string.uploaded_by, neko.uploader.username)
            view.tvApproved.text = itemView.context.getString(R.string.approved_by, neko.approver?.username ?: "-")
            view.tvNekoFavorites.text = itemView.context.getString(R.string.neko_favorites, neko.favorites)
            view.tvNekoLikes.text = itemView.context.getString(R.string.neko_likes, neko.likes)
            view.tvArtist.text = itemView.context.getString(R.string.neko_artist, neko.artist)
            view.tvTags.text = itemView.context.getString(R.string.neko_tags, neko.tags.joinToString(", "))
            view.tvTags.movementMethod = ScrollingMovementMethod()

            Glide.with(itemView.context)
                .load(neko.getImageUrl())
                .centerCrop()
                .into(view.fullNekoImg)

            view.fullNekoImg.onClick {
                ImageViewer.Builder(itemView.context, arrayOf(neko.getImageUrl())).show()
            }

            if (isLoggedin) {
                if (user == null)
                    return@onClick

                val liked = user?.likes?.find { id -> id == neko.id }
                val faved = user?.favorites?.find { id -> id == neko.id }
                view.btnLikeNeko.text = if (liked.isNullOrBlank()) "Like" else "Unlike"
                view.btnFavNeko.text = if (faved.isNullOrBlank()) "Favorite" else "Unfavorite"
            }

            val token = sharedPreferences.getString("token", "") ?: ""
            view.btnLikeNeko.onClick {
                if (isLoggedin) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val likedNeko = user?.likes?.find { id -> id == neko.id }
                        val reqbodystr =
                            if (likedNeko.isNullOrBlank()) "{\"create\": true, \"type\": \"like\"}"
                            else "{\"create\": false, \"type\": \"like\"}"

                        val reqbody = RequestBody.create(MediaType.parse("application/json"), reqbodystr)
                        val requrl = "https://nekos.moe/api/v1/image/${neko.id}/relationship"

                        val headers = okhttp3.Headers.Builder()
                            .add("Authorization", token)
                            .add("User-Agent", Api.userAgent)
                            .add("Content-Type", "application/json;charset=utf-8")
                            .build()

                        val request = Request.Builder()
                            .url(requrl)
                            .headers(headers)
                            .patch(reqbody)
                            .build()

                        try {
                            val response = httpClient.newCall(request).execute()
                            if (!response.isSuccessful || response.code() > 204) {
                                val msg = if (likedNeko.isNullOrBlank()) "Failed to like" else "Failed to unlike"
                                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                            } else {
                                if (likedNeko.isNullOrBlank()) {
                                    user?.likes?.add(neko.id)
                                    sharedPreferences.edit().putString("user", Api.gson.toJson(user!!)).apply()
                                    Snackbar.make(view, "Liked", Snackbar.LENGTH_SHORT).show()
                                    GlobalScope.launch(Dispatchers.Main) { view.btnLikeNeko.text = "Unlike" }
                                } else {
                                    user?.likes?.remove(neko.id)
                                    sharedPreferences.edit().putString("user", Api.gson.toJson(user!!)).apply()
                                    Snackbar.make(view, "Unliked", Snackbar.LENGTH_SHORT).show()
                                    GlobalScope.launch(Dispatchers.Main) { view.btnLikeNeko.text = "Like" }
                                }
                            }
                            response.close()
                        } catch (e: IOException) {
                            Snackbar.make(view, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Snackbar.make(view, "Login to use this action", Snackbar.LENGTH_LONG).show()
                }
            }

            view.btnFavNeko.onClick {
                if (isLoggedin) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val favedNeko = user?.favorites?.find { id -> id == neko.id }
                        val reqbodystr =
                            if (favedNeko.isNullOrBlank()) "{\"create\": true, \"type\": \"favorite\"}"
                            else "{\"create\": false, \"type\": \"favorite\"}"

                        val reqbody = RequestBody.create(MediaType.parse("application/json"), reqbodystr)
                        val requrl = "https://nekos.moe/api/v1/image/${neko.id}/relationship"

                        val headers = okhttp3.Headers.Builder()
                            .add("Authorization", token)
                            .add("User-Agent", Api.userAgent)
                            .build()

                        val request = Request.Builder()
                            .url(requrl)
                            .headers(headers)
                            .patch(reqbody)
                            .build()

                        try {
                            val response = httpClient.newCall(request).execute()
                            if (!response.isSuccessful || response.code() > 204) {
                                val msg = if (favedNeko.isNullOrBlank()) "Failed to favorite" else "Failed to unfavorite"
                                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                            } else {
                                if (favedNeko.isNullOrBlank()) {
                                    user?.favorites?.add(neko.id)
                                    sharedPreferences.edit().putString("user", Api.gson.toJson(user!!)).apply()
                                    Snackbar.make(view, "Favorited", Snackbar.LENGTH_SHORT).show()
                                    GlobalScope.launch(Dispatchers.Main) { view.btnFavNeko.text = "Unfavorite" }
                                } else {
                                    user?.favorites?.remove(neko.id)
                                    sharedPreferences.edit().putString("user", Api.gson.toJson(user!!)).apply()
                                    Snackbar.make(view, "Unfavorited", Snackbar.LENGTH_SHORT).show()
                                    GlobalScope.launch(Dispatchers.Main) { view.btnFavNeko.text = "Favorite" }
                                }
                            }
                            response.close()
                        } catch (e: IOException) {
                            Snackbar.make(view, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Snackbar.make(view, "Login to use this action", Snackbar.LENGTH_LONG).show()
                }
            }

            view.btnCloseNeko.onClick {
                nekoDialog.dismiss()
            }

            view.btnShareNeko.onClick {
                if (!connected || !isConnected(itemView.context)) return@onClick

                picasso.load(neko.getImageUrl()).into {
                    onFailed { e, _ ->
                        Snackbar.make(view, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
                    }
                    onLoaded { bitmap, _ ->
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "image/png"

                        val bytes = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)

                        val sdCard = Environment.getExternalStorageDirectory()
                        val dir = File(sdCard.absolutePath + "/Nekos")
                        if (dir.exists().not())
                            dir.mkdirs()

                        file = File(dir, "share-${neko.id}.png")

                        try {
                            file.createNewFile()
                            val fo = FileOutputStream(file)
                            fo.write(bytes.toByteArray())
                            fo.flush()
                            fo.close()
                        } catch (e: IOException) {
                            val message = e.message ?: "Unable to save/share image"
                            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                        }

                        val uri = FileProvider.getUriForFile(view.context, view.context.applicationContext.packageName + ".NekoFileProvider", file)
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        intent.putExtra(Intent.EXTRA_TEXT, "Artist: ${neko.artist}\n" +
                                "Tags: ${neko.tags.subList(0, 5).joinToString(", ")}\n" +
                                "#catgirls #nekos\n" +
                                "https://nekos.moe/post/${neko.id}")

                        startActivityForResult(view.context as MainActivity, Intent.createChooser(intent,"Share Image"),997, null)

                        file.deleteOnExit()
                    }
                }
            }

            view.btnSaveNeko.onClick {
                if (!connected || ! isConnected(view.context)) return@onClick

                if (!hasPermissions(view.context, permissions)) {
                    ActivityCompat.requestPermissions(view.context as MainActivity, permissions, 999)
                } else {
                    downloadAndSave(neko, view)
                }
            }

            nekoDialog.show()
        }
    }
}

private fun downloadAndSave(neko: Neko, view: View) {
    val mediaStorageDir = File(Environment.getExternalStorageDirectory().toString() + "/Nekos/")
    if (!mediaStorageDir.exists()) mediaStorageDir.mkdirs()
    val file = File(mediaStorageDir, "${neko.id}.jpeg")

    Fuel.download("https://nekos.moe/image/${neko.id}").fileDestination { response, _ ->
        response.toString()
        file
    }.response { _, _, result ->
        val (data, err) = result
        when {
            data != null -> {
                val fileOutput = FileOutputStream(file)
                fileOutput.write(data, 0, data.size)

                view.context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                Snackbar.make(view, "Saved as ${neko.id}.jpeg", Snackbar.LENGTH_SHORT).show()
                fileOutput.close()
            }
            err != null -> Snackbar.make(view, err.message ?: "ERROR", Snackbar.LENGTH_LONG).show()
        }
    }
}