package com.example.myapplication.ui.gallery

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentGalleryBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Date
import java.util.Locale
import kotlin.properties.Delegates


data class Album(var albumName: String, var images: List<String>)
class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val gson = Gson()
    private var currentAlbumIndex = -1 // 현재 그리드뷰로 보여지는 앨범이 albums에서 몇 번째 index인지를 나타낸다. (-1은 "전체 사진"을 가리킨다)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        ViewModelProvider(this)[GalleryViewModel::class.java]

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val gridView: GridView = binding.galleryGridView
        val cameraButton = binding.galleryCameraAppFloatingButton
        val menuButton = binding.galleryMenuImageButton
        val albumTitleTextView = binding.galleryAlbumTitleTextView

        val uriArr = getAllPhotos()
        val gridViewAdapter = GridAdapter(requireContext(), uriArr) // 사진 목록을 adapter에 전달한 후,
        gridView.adapter =  gridViewAdapter                         // adapter를 gridView에 연결한다.

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ -> // 사진 하나를 클릭하면 실행된다.
            showImagePopup(requireContext(), uriArr, position, gridViewAdapter)
        }

        cameraButton.setOnClickListener { // 버튼을 누르면 카메라 앱을 실행한다.
            Toast.makeText(requireContext(), "카메라 실행", Toast.LENGTH_SHORT).show()
            executeCameraApp()
        }

        menuButton.setOnClickListener {view -> // 버튼을 누르면 메뉴가 나타난다.
            showPopupMenu(view, uriArr, gridViewAdapter, albumTitleTextView)
        }

        return root
    }

    // 1. SD 저장소에서 사진 주소 가져오기
    private fun getAllPhotos(
        sortOrder: String = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC",
        prevUriArr: ArrayList<String>? = null): ArrayList<String> { // 저장소에 있는 모든 사진들의 uri 주소들을 uriArr에 저장한 후 반환한다.
        val cursor = activity?.contentResolver?.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, sortOrder)
        val uriArr = ArrayList<String>()
        if (cursor != null) {
            while (cursor.moveToNext()) { // 사진 경로 Uri 가져오기
                val uri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                if (prevUriArr == null || prevUriArr.contains(uri)) { // null은 앨범 구분이 없는 상태이다. (전체 사진을 uriArr에 담는다)
                    uriArr.add(uri)                                   // 만약 prevUriArr을 parameter로 받으면, 앨범 안에 있는 사진에 대해서만 uriArr에 추가한다.
                }
            }
            cursor.close()
        }
        return uriArr
    }

    // 2. 사진 클릭 시 사진을 확대해서 Dialog로 보여주기
    private fun showImagePopup(context: Context, uriArr: ArrayList<String>, position: Int, adapter: GridAdapter) { // 사진을 누르면 크게 보여준다
        val imagePath = uriArr[position]
        val builderShow = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.fragment_gallery_pick, null)
        builderShow.setView(dialogView)
        val imageView = dialogView.findViewById<ImageView>(R.id.galleryPickedImageView)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE

        Glide.with(context).load(imagePath).into(imageView)

        val albumFile = "albums.json"
        copyAssetToFile(requireContext(), albumFile)
        val albums = readFromFile(albumFile)
        val albumNames: MutableList<String> = mutableListOf()
        for (album in albums) {
            albumNames.add(album.albumName)
        }

        // 1. 창 닫기
        builderShow.setPositiveButton("닫기") {dialog, _ -> dialog.dismiss()}

        // 2. 앨범으로 복사하기
        builderShow.setNegativeButton("앨범") {_, _ ->
            val builderAlbum = AlertDialog.Builder(context) // 앨범 이동을 위한 새로운 창을 띄운다.
            builderAlbum.setTitle("앨범으로 복사")

            var selectedAlbumPosition by Delegates.notNull<Int>()
            builderAlbum.setSingleChoiceItems(albumNames.toTypedArray(), -1) {_, position -> // 항목을 고르면 앨범 이름을 기억한다.
                selectedAlbumPosition = position
            }
            builderAlbum.setPositiveButton("복사") {_, _ -> // 복사 버튼을 누르면 해당 사진을 해당 앨범에 추가한다.
                val updatedImages: MutableList<String> = albums[selectedAlbumPosition].images as MutableList<String>
                updatedImages.add(imagePath)
                albums[selectedAlbumPosition].images = updatedImages
                writeToFile(albumFile, albums)
                val selectedAlbumName = albumNames[selectedAlbumPosition]
                Toast.makeText(context, "$selectedAlbumName 앨범으로 복사 완료", Toast.LENGTH_SHORT).show()

                if (currentAlbumIndex == selectedAlbumPosition) { // 현재 그리드뷰로 나타난 앨범으로 복사한 경우 그리드뷰를 업데이트한다.
                    uriArr.add(imagePath)
                    adapter.notifyDataSetChanged()
                }
            }
            builderAlbum.setNegativeButton("취소") {dialog, _ -> dialog.dismiss()}
            builderAlbum.create().show()
        }

        // 3. 삭제하기
        builderShow.setNeutralButton("삭제") {_, _ ->
            if (currentAlbumIndex == -1) { // "전체 사진"인 상태에서 사진을 삭제하는 경우 실제 저장소에서도 사진을 삭제한다.
                val builderDeleteAtStorage = AlertDialog.Builder(context) // 삭제 확인을 위한 새로운 창을 띄운다.
                builderDeleteAtStorage.setTitle("저장소에서 삭제하기")
                builderDeleteAtStorage.setMessage("정말로 해당 파일을 저장소에서 삭제하시겠습니까?")

                builderDeleteAtStorage.setPositiveButton("삭제") { _, _ ->
                    if (deletePhoto(imagePath)) {       // 저장소에서 파일을 실제로 삭제한 후에
                        uriArr.removeAt(position)       // 사진들의 주소 목록인 uriArr에서도 삭제해서
                        adapter.notifyDataSetChanged()  // Adapter에도 삭제했음을 알린다. -> 그리드뷰에 곧바로 반영이 되어서 나타난다.
                        Toast.makeText(context, "저장소에서 삭제 성공!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "삭제 실패?", Toast.LENGTH_SHORT).show()
                    }
                }
                builderDeleteAtStorage.setNegativeButton("취소") {dialog, _ -> dialog.dismiss()}
                builderDeleteAtStorage.create().show()
            }
            else { // 그리드뷰가 앨범인 상태에서 사진을 삭제하는 경우 앨범에서만 삭제한다. (저장소에서는 유지된다)
                val albumName = albumNames[currentAlbumIndex]
                val builderDeleteAtAlbum = AlertDialog.Builder(context) // 삭제 확인을 위한 새로운 창을 띄운다.
                builderDeleteAtAlbum.setTitle("앨범 $albumName" + "에서 삭제하기")
                builderDeleteAtAlbum.setMessage("정말로 해당 파일을 $albumName 앨범에서 삭제하시겠습니까?")

                builderDeleteAtAlbum.setPositiveButton("삭제") { _, _ ->
                    val updatedImages: MutableList<String> = albums[currentAlbumIndex].images as MutableList<String>
                    updatedImages.remove(imagePath)
                    albums[currentAlbumIndex].images = updatedImages
                    writeToFile(albumFile, albums)  // 해당 사진을 현재 앨범 목록에서 제거한 후에
                    uriArr.removeAt(position)       // 사진들의 주소 목록인 uriArr에서도 삭제해서
                    adapter.notifyDataSetChanged()  // Adapter에도 삭제했음을 알린다. -> 그리드뷰에 곧바로 반영이 되어서 나타난다.
                    Toast.makeText(context, "앨범에서 삭제 성공!", Toast.LENGTH_SHORT).show()
                }
                builderDeleteAtAlbum.setNegativeButton("취소") {dialog, _ -> dialog.dismiss()}
                builderDeleteAtAlbum.create().show()
            }
        }
        builderShow.create().show()
    }

    // 3. SD 저장소에서 사진 삭제하기
    private fun deletePhoto(imagePath: String): Boolean { // 삭제 버튼을 누르면 해당 사진을 저장소에서 삭제한다.
        fun getImageId(imagePath: String): Long? { // 보조 함수
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val selection = MediaStore.Images.Media.DATA + "=?"
            val selectionArgs = arrayOf(imagePath)
            val cursor = requireContext().contentResolver.query(uri, projection, selection, selectionArgs, null)
            val imageId = if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                cursor.getLong(columnIndex)
            } else {
                null
            }
            cursor?.close()
            return imageId
        }

        val resolver = requireContext().contentResolver
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { // 안드로이드 낮은 버전에서는 기존 방식대로 한다.
            val selection = MediaStore.Images.Media.DATA + "=?"
            val selectionArgs = arrayOf(imagePath)
            val deletedRows = resolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs)
            deletedRows > 0
        } else { // 안드로이드 높은 버전에서는 새로운 방식대로 한다.
            val imageId = getImageId(imagePath) // imagePath로부터 이미지의 ID를 가져옴
            if (imageId != null && imageId > -1) {
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId)
                val deletedRows = resolver.delete(uri, null, null)
                deletedRows > 0
            } else {
                false
            }
        }
    }


    private lateinit var currentPhotoPath: String
    
    // 4. 카메라 앱 실행하기
    private fun executeCameraApp() { // 플로팅 카메라 버튼을 누르면 카메라 앱을 실행한다.
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        activityResult.launch(takePictureIntent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        val storageDir: File? = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun saveImageToFile(imageBitmap: Bitmap) {
        /*val fileOutputStream: FileOutputStream?
        val file = createImageFile() // 이미지 파일을 생성하기 위한 메서드 호출
        try {
            fileOutputStream = FileOutputStream(file)
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.close()
            Toast.makeText(requireContext(), "이미지가 저장되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "이미지 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        } */
        var folderPath = MediaStore.Images.Media.DATA
        var fileName = "comment.jpeg"

        var folder = File(folderPath)
        if (!folder.isDirectory) folder.mkdirs()

        var out = FileOutputStream(folderPath + fileName)

        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }

    private val activityResult: ActivityResultLauncher<Intent> = registerForActivityResult( // 카메리 앱 실행 후 사진 저장하기
        ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == RESULT_OK && it.data != null) {
            // val extras = it.data!!.extras // 값 담기

            val imageBitmap = it.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                //saveImageToFile(imageBitmap)

                val imageFileName = "MY_IMG_${System.currentTimeMillis()}.jpg"
                val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

// 이미지 파일을 저장할 실제 파일 객체 생성
                val imageFile = File(storageDir, imageFileName)

// 이미지 파일의 Uri 생성
                val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Android N 이상 버전부터는 FileProvider를 사용하여 Uri를 생성해야 함
                    FileProvider.getUriForFile(requireContext(), "com.example.android.fileprovider", imageFile)
                } else {
                    Uri.fromFile(imageFile)
                }

// 이미지 파일 저장
                val outputStream: OutputStream? = requireContext().contentResolver.openOutputStream(imageUri)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream?.close()

// 갤러리 앱에서 새로 저장된 이미지를 인식할 수 있도록 갤러리 스캔
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = imageUri
                requireContext().sendBroadcast(mediaScanIntent)


            }
            Toast.makeText(requireContext(), "저장 완료!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "저장 취소?", Toast.LENGTH_SHORT).show()
        }
    }

    // 팝업 메뉴 띄우기
    private fun showPopupMenu(view: View, uriArr: ArrayList<String>, adapter: GridAdapter, albumTitle: TextView) { // 정렬 버튼을 누르면 팝업 메뉴가 나타난다. 선택한 item에 따라 사진이 정렬된다.
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.gallery_change_order_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                // 1. 메뉴에서 "정렬" 항목을 클릭할 경우
                R.id.gallery_menu_order -> {
                    val builderOrder = AlertDialog.Builder(context) // 정렬을 위한 새로운 창을 띄운다.
                    builderOrder.setTitle("정렬하기")

                    var selectedOrderPosition by Delegates.notNull<Int>()
                    val orderList = arrayOf("이름 (가나다순)", "이름 (가나다 역순)", "날짜 (오름차순)", "날짜 (내림차순)")
                    builderOrder.setSingleChoiceItems(orderList, -1) {_, position -> // 항목을 고르면 앨범 이름을 기억한다.
                        selectedOrderPosition = position
                    }
                    builderOrder.setPositiveButton("확인") {_, _ -> // 선택한 방식으로 정렬하기
                        changeGridViewOrder(selectedOrderPosition, uriArr, adapter)
                    }
                    builderOrder.setNegativeButton("취소") {dialog, _ -> dialog.dismiss()}
                    builderOrder.create().show()
                    true
                }

                // 2. 메뉴에서 "앨범 선택" 항목을 클릭할 경우
                R.id.gallery_menu_album_select -> {
                    // 앨범 선택을 위한 새로운 창을 띄운다.
                    val builderAlbum = AlertDialog.Builder(context)
                    builderAlbum.setTitle("앨범 선택")

                    val albumFile = "albums.json"
                    copyAssetToFile(requireContext(), albumFile)
                    val albums = readFromFile(albumFile)
                    val albumNames: MutableList<String> = mutableListOf("전체 사진") // 전체 사진을 위한 항목을 맨 앞에 하나 추가한다.
                    for (album in albums) {
                        albumNames.add(album.albumName)
                    }

                    var selectedAlbumPosition = 0 // 항목을 고르면 앨범 순서를 기억한다.
                    builderAlbum.setSingleChoiceItems(albumNames.toTypedArray(), 0) {_, position ->
                        selectedAlbumPosition = position
                    }

                    builderAlbum.setPositiveButton("확인") {_, _ -> // 확인 버튼을 누르면 해당 앨범을 보여준다
                        val selectedAlbumImages = if (selectedAlbumPosition == 0) { // 0은 앨범 없이 전체 사진을 가리킨다.
                            getAllPhotos()
                        } else {
                            albums[selectedAlbumPosition-1].images // position=0인 위치에 "전체 사진" 항목이 추가되었으므로 index를 1씩 뺀다.
                        }
                        uriArr.clear()
                        uriArr.addAll(selectedAlbumImages)
                        adapter.notifyDataSetChanged()
                        albumTitle.text = albumNames[selectedAlbumPosition] // 상단의 앨범 이름을 표시하는 TextView에도 반영
                        currentAlbumIndex = selectedAlbumPosition-1
                        Toast.makeText(context, "앨범 선택 완료", Toast.LENGTH_SHORT).show()
                    }

                    builderAlbum.setNeutralButton("앨범 삭제", null)
                    builderAlbum.setNegativeButton("이름 변경", null)
                    val createdAlbum = builderAlbum.create()

                    createdAlbum.setOnShowListener {dialog ->
                        // 앨범 삭제 버튼
                        val neutralButton = createdAlbum.getButton(AlertDialog.BUTTON_NEUTRAL)
                        neutralButton.setOnClickListener {
                            if (selectedAlbumPosition == 0) { // "전체 사진"을 고르면, 아무 일도 일어나지 않는다.
                                Toast.makeText(context, "전체 사진은 앨범이 아닙니다.\n(삭제 불가)", Toast.LENGTH_SHORT).show()
                            } else {
                                dialog.dismiss()
                                // 해당 앨범을 삭제하기 위한 새로운 창을 띄운다.
                                val builderDeleteAlbum = AlertDialog.Builder(requireContext())
                                val deletedAlbumName = albumNames[selectedAlbumPosition]
                                builderDeleteAlbum.setTitle("앨범 삭제")
                                builderDeleteAlbum.setMessage("정말로 $deletedAlbumName 앨범을 삭제하시겠습니까?")

                                builderDeleteAlbum.setPositiveButton("삭제") { _, _ ->
                                    albums.removeAt(selectedAlbumPosition - 1)
                                    writeToFile(albumFile, albums)
                                    Toast.makeText(context, "$deletedAlbumName 앨범 삭제 완료", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                    
                                    if (albumTitle.text == albumNames[selectedAlbumPosition]) { // 현재 그리드뷰에서 보여지고 있는 앨범을 삭제한 경우
                                        uriArr.clear()                                          // 그리드뷰를 "전체 사진"으로 전환한다.
                                        uriArr.addAll(getAllPhotos())
                                        adapter.notifyDataSetChanged()
                                        albumTitle.text = "전체 사진" // 상단의 앨범 이름을 표시하는 TextView에도 반영
                                        currentAlbumIndex = -1
                                    }
                                }
                                builderDeleteAlbum.setNegativeButton("취소") {dialog, _ ->
                                    dialog.dismiss()
                                }

                                builderDeleteAlbum.create().show()
                            }
                        }
                        // 앨범 이름 변경 버튼
                        val negativeButton = createdAlbum.getButton(AlertDialog.BUTTON_NEGATIVE)
                        negativeButton.setOnClickListener {
                            if (selectedAlbumPosition == 0) { // "전체 사진"을 고르면, 아무 일도 일어나지 않는다.
                                Toast.makeText(context, "전체 사진은 앨범이 아닙니다.\n(이름 변경 불가)", Toast.LENGTH_SHORT).show()
                            } else {
                                dialog.dismiss() // 기존의 앨범 선택 창은 닫는다.
                                // 앨범 이름을 바꾸기 위한 새로운 창을 띄운다.
                                val builderModifyName = AlertDialog.Builder(requireContext())
                                builderModifyName.setTitle("앨범 이름 변경")
                                val addAlbumView = layoutInflater.inflate(R.layout.gallery_add_album, null)
                                builderModifyName.setView(addAlbumView)

                                builderModifyName.setNegativeButton("취소") {dialog, _ -> dialog.dismiss()}
                                builderModifyName.setPositiveButton("변경", null) // 입력한 이름을 검증한 후에, 앨범 이름을 변경한다
                                val createdModifyName = builderModifyName.create()

                                createdModifyName.setOnShowListener { dialog ->
                                    val positiveButton = createdModifyName.getButton(AlertDialog.BUTTON_POSITIVE)
                                    positiveButton.setOnClickListener {
                                        val newAlbumName = addAlbumView.findViewById<EditText>(R.id.gallery_make_new_album_TextView).text.toString()
                                        if (newAlbumName.isEmpty()) {
                                            Toast.makeText(context, "앨범 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                                        }
                                        else if (albumNames.contains(newAlbumName)) {
                                            Toast.makeText(context, "앨범 이름이 중복됩니다.", Toast.LENGTH_SHORT).show()
                                        }
                                        else {
                                            val prevAlbumImages = albums[selectedAlbumPosition-1].images
                                            val newAlbum = Album(newAlbumName, prevAlbumImages)
                                            albums.removeAt(selectedAlbumPosition-1)
                                            albums.add(selectedAlbumPosition-1, newAlbum)
                                            writeToFile(albumFile, albums)
                                            Toast.makeText(context, "앨범 이름 변경 성공", Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                        }
                                    }
                                }
                                createdModifyName.show()
                            }
                        }
                    }
                    createdAlbum.show()
                    true
                }

                // 3. 메뉴에서 "앨범 생성" 항목을 클릭할 경우
                R.id.gallery_menu_album_make -> {
                    val albumFile = "albums.json"
                    copyAssetToFile(requireContext(), albumFile)
                    val albums = readFromFile(albumFile)
                    val albumNames: MutableList<String> = mutableListOf()
                    for (album in albums) {
                        albumNames.add(album.albumName)
                    }

                    // 새로운 앨범을 생성하기 위한 새로운 창을 띄운다.
                    val builderNewAlbum = AlertDialog.Builder(requireContext())
                    builderNewAlbum.setTitle("새로운 앨범 생성")
                    val addAlbumView = layoutInflater.inflate(R.layout.gallery_add_album, null)
                    builderNewAlbum.setView(addAlbumView)

                    builderNewAlbum.setNegativeButton("취소") {dialog, _ -> dialog.dismiss()}
                    builderNewAlbum.setPositiveButton("생성", null) // 입력한 이름을 검증한 후에, 앨범을 생성한다
                    val createdNewAlbum = builderNewAlbum.create()

                    createdNewAlbum.setOnShowListener { dialog ->
                        val positiveButton = createdNewAlbum.getButton(AlertDialog.BUTTON_POSITIVE)
                        positiveButton.setOnClickListener {
                            val newAlbumName = addAlbumView.findViewById<EditText>(R.id.gallery_make_new_album_TextView).text.toString()
                            if (newAlbumName.isEmpty()) {
                                Toast.makeText(context, "앨범 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                            }
                            else if (albumNames.contains(newAlbumName)) {
                                Toast.makeText(context, "앨범 이름이 중복됩니다.", Toast.LENGTH_SHORT).show()
                            }
                            else {
                                val newAlbum = Album(newAlbumName, emptyList())
                                albums.add(newAlbum)
                                writeToFile(albumFile, albums)
                                Toast.makeText(context, "앨범 생성 성공", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                        }
                    }
                    createdNewAlbum.show()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    // 팝업 메뉴 중 정렬 기능을 위한 함수 (선택한 앨범에 대해서도 정렬 가능)
    private fun changeGridViewOrder(position: Int, uriArr: ArrayList<String>, adapter: GridAdapter) {
        // orderList = ["이름 (가나다순)", "이름 (가나다 역순)", "날짜 (오름차순)", "날짜 (내림차순)"]
        when(position) {
            0 -> {
                val orderedUriArr = getAllPhotos(MediaStore.Images.ImageColumns.DISPLAY_NAME + " ASC", uriArr)
                uriArr.clear()
                uriArr.addAll(orderedUriArr)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "이름 가나다순 정렬", Toast.LENGTH_SHORT).show()
            }
            1 -> {
                val orderedUriArr = getAllPhotos(MediaStore.Images.ImageColumns.DISPLAY_NAME + " DESC", uriArr)
                uriArr.clear()
                uriArr.addAll(orderedUriArr)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "이름 역순 정렬", Toast.LENGTH_SHORT).show()
            }
            2 -> {
                val orderedUriArr = getAllPhotos(MediaStore.Images.ImageColumns.DATE_TAKEN + " ASC", uriArr)
                uriArr.clear()
                uriArr.addAll(orderedUriArr)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "날짜 오름차순 정렬", Toast.LENGTH_SHORT).show()
            }
            3 -> {
                val orderedUriArr = getAllPhotos(MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC", uriArr)
                uriArr.clear()
                uriArr.addAll(orderedUriArr)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "날짜 내림차순 정렬", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // json 파일 읽고 쓰기 관련 함수
    private fun copyAssetToFile(context: Context, fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            return
        }

        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)

        inputStream.close()
        outputStream.close()
    }

    private fun writeToFile(fileName: String, people: List<Album>) {
        val file = File(requireContext().filesDir, fileName)
        val json = gson.toJson(people)
        file.writeText(json)
    }

    private fun readFromFile(fileName: String): MutableList<Album> {
        val file = File(requireContext().filesDir, fileName)
        val json = file.readText()
        return gson.fromJson(json, object : TypeToken<MutableList<Album>>() {}.type)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
/*
* <한 일>
* - 사진 정렬 기능
* - 앨범 기능 (종료해도 유지, 삭제하면 날라감)
* - 앨범 생성 기능
* - 앨범 이름 검증 기능
* - 앨범에 따른 정렬 기능
* - 앨범 이름 변경 기능
* - 앨범 삭제 기능
* - 사진 삭제 기능 (전체 사진에서 삭제할 경우 저장소에서도 삭제되지만, 앨범에서 삭제할 경우 앨범에서만 삭제된다)
*
* <해야 할 일>
* - 카메라 촬영 후 저장소에 저장 구현
*
* 
* <새로운 기능>
* - 위치 기능
* - 주소록 태그 기능
*
* */