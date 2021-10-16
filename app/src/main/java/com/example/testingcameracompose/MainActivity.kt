package com.example.testingcameracompose

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.rememberImagePainter
import com.example.testingcameracompose.camara.CameraCapture
import com.example.testingcameracompose.fireStorage.FireStorageRepo
import com.example.testingcameracompose.fireStorage.ImageItem
import com.example.testingcameracompose.gallery.GallerySelect
import com.example.testingcameracompose.ui.theme.TestingCameraComposeTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @ExperimentalFoundationApi
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestingCameraComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    MainContent(Modifier.fillMaxSize())
                }
            }
        }
    }
}

val EMPTY_IMAGE_URI: Uri = Uri.parse("file://dev/null")

@ExperimentalFoundationApi
@ExperimentalPermissionsApi
@Composable
fun MainContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showUploadedImages by remember { mutableStateOf(true) }
    var imageUri by remember { mutableStateOf(EMPTY_IMAGE_URI) }
    var fireStorageRepo by remember { mutableStateOf(FireStorageRepo()) }
    var scope = rememberCoroutineScope()

    if (showUploadedImages) {
        UploadedImageContent(modifier = modifier) {
            showUploadedImages = it
        }
    } else {
        // User wants to either take a photo or pick one from gallery
        if (imageUri != EMPTY_IMAGE_URI) {

            // Preview Image to upload
            Box(modifier = modifier) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = rememberImagePainter(imageUri),
                    contentDescription = "Captured image"
                )
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier.padding(4.dp),
                        onClick = {
                            imageUri = EMPTY_IMAGE_URI
                        }
                    ) {
                        Text("Re-take")
                    }
                    Button(
                        modifier = Modifier.padding(4.dp),
                        onClick = {
                            // logic for uploading
                            scope.launch {
                                fireStorageRepo.uploadImageToStorage(
                                    context,
                                    imageUri,
                                    imageUri.lastPathSegment!!
                                ).join()
                            }.invokeOnCompletion {
                                imageUri = EMPTY_IMAGE_URI
                                showUploadedImages = true
                            }
                        }
                    ) {
                        Text("Upload")
                    }
                }
            }
        } else {
            var showGallerySelect by remember { mutableStateOf(false) }

            if (showGallerySelect) {
                GallerySelect(
                    modifier = modifier,
                    onImageUri = { uri ->
                        showGallerySelect = false
                        imageUri = uri
                    }
                )
            } else {
                Box(modifier = modifier) {
                    CameraCapture(
                        modifier = modifier,
                        onImageFile = { file ->
                            imageUri = file.toUri()
                        }
                    )
                    Row(
                        modifier = Modifier.align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            modifier = Modifier
                                .padding(4.dp),
                            onClick = {
                                showGallerySelect = true
                            }
                        ) {
                            Text("Pick From Gallery")
                        }
                        Button(
                            modifier = Modifier
                                .padding(4.dp),
                            onClick = {
                                showUploadedImages = !showUploadedImages
                            }
                        ) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun UploadedImageContent(
    modifier: Modifier,
    onUploadButtonPressed: (toggle: Boolean) -> Unit
) {
    var fireStorageRepo by remember { mutableStateOf(FireStorageRepo()) }

    var shouldUpdate by remember { mutableStateOf(false) }
    var imageItems = produceState(initialValue = mutableListOf<ImageItem>(), shouldUpdate) {
        value = fireStorageRepo.listFiles()
    }

    var scope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        stickyHeader {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    modifier = Modifier
                        .padding(4.dp),
                    onClick = {
                        onUploadButtonPressed(false)
                    }
                ) {
                    Text("Upload an image")
                }
            }
        }

        item {
            Text(
                text = "My FireStorage Images",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h4
            )
        }

        items(imageItems.value.size) { index ->
            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .border(2.dp, Color.LightGray, RoundedCornerShape(2.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier
                        .fillParentMaxWidth(1f)
                        .aspectRatio(1f)
                        .padding(4.dp),
                    painter = rememberImagePainter(imageItems.value[index].url),
                    contentDescription = "Captured image"
                )
                Button(onClick = {
                    scope.launch {
                        fireStorageRepo.deleteImage(
                            context,
                            imageItems.value[index].fileName
                        ).join()
                    }.invokeOnCompletion {
                        shouldUpdate = !shouldUpdate
                    }
                }) {
                    Text(text = "Delete")
                }
            }
        }
    }

}



