/*
 *  Copyright (c) 2018 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.deliciousfood


import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.mindorks.paracamera.Camera
import android.widget.Toast
import android.content.Intent
import android.graphics.Bitmap
import kotlinx.android.synthetic.main.activity_main.*
import android.content.pm.PackageManager
import android.graphics.Color
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions
import android.support.v4.app.ActivityCompat
import com.google.firebase.FirebaseApp
import com.raywenderlich.deliciousfood.R.id.*

class MainActivity : AppCompatActivity() {

  private lateinit var camera: Camera

  companion object {

    private const val WRITE_PERMISSION_REQUEST_CODE = 1

  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    FirebaseApp.initializeApp(this)

    camera = Camera.Builder()
                   .resetToCorrectOrientation(true)
                   .setTakePhotoRequestCode(Camera.REQUEST_TAKE_PHOTO)
                   .setDirectory("pics")
                   .setName("delicious_${System.currentTimeMillis()}")
                   .setImageFormat(Camera.IMAGE_JPEG)
                   .setCompression(75)
                   .build(this)

  }

  fun takePicture(view: View){

    if (!hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
        !hasPermission(android.Manifest.permission.CAMERA)) {

      ActivityCompat.requestPermissions(this,
          arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA), WRITE_PERMISSION_REQUEST_CODE)
      return
    }

    try {
      camera.takePicture()
    }catch (e: Exception){
      Toast.makeText(this.applicationContext, getString(R.string.error_taking_picture), Toast.LENGTH_SHORT).show()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == Camera.REQUEST_TAKE_PHOTO) {
      val bitmap = camera.cameraBitmap
      if (bitmap != null) {
        imageView.setImageBitmap(bitmap)
        detectDeliciousFood(bitmap)
      } else {
        Toast.makeText(this.applicationContext, getString(R.string.picture_not_taken), Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    camera.deleteImage()
  }

  fun detectDeliciousFood(bitmap: Bitmap) {

    progressBar.visibility = View.VISIBLE

    val options = FirebaseVisionLabelDetectorOptions.Builder()
        .setConfidenceThreshold(0.8f)
        .build()
    val image = FirebaseVisionImage.fromBitmap(bitmap)
    val detector = FirebaseVision.getInstance()
        .getVisionLabelDetector(options)

    detector.detectInImage(image)
        .addOnSuccessListener {

          progressBar.visibility = View.INVISIBLE

          if (hasDeliciousFood(it.map { it.label.toString() })){
            displayResultMessage(true)
          }else{
            displayResultMessage(false)
          }

        }
        .addOnFailureListener {
          progressBar.visibility = View.INVISIBLE
          Toast.makeText(this.applicationContext, "Error", Toast.LENGTH_SHORT).show()

        }
  }

  fun displayResultMessage(hasDeliciousFood:Boolean){

    responseCardView.visibility = View.VISIBLE

    if (hasDeliciousFood){
      responseCardView.setCardBackgroundColor(Color.GREEN)
      responseTextView.text = getString(R.string.delicious_food)
    }else{
      responseCardView.setCardBackgroundColor(Color.RED)
      responseTextView.text = getString(R.string.not_delicious_food)
    }
  }

  fun hasDeliciousFood(items: List<String>):Boolean{

    for (result in items){
      if (result.contains("Food", true))
        return true
    }
    return false
  }

  fun hasPermission(permission: String):Boolean{
    return ActivityCompat.checkSelfPermission(this,
        permission) == PackageManager.PERMISSION_GRANTED

  }
}