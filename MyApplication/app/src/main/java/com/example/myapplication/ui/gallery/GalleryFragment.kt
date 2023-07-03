package com.example.myapplication.ui.gallery

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentGalleryBinding
import java.io.File

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        ViewModelProvider(this)[GalleryViewModel::class.java]

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val gridView: GridView = binding.galleryGridView

        val uriArr = getAllPhotos()
        gridView.adapter = GridAdapter(requireContext(), uriArr) // 사진 목록을 adapter로 전달한 후, adapter을 gridView에 연결

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            showImagePopup(requireContext(), uriArr, position, gridView.adapter as GridAdapter)
        }
        return root
    }

    private fun getAllPhotos(): ArrayList<String> { // 저장소에 있는 모든 사진들의 uri 주소들을 uriArr에 저장한 후 반환한다.
        val cursor = activity?.contentResolver?.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
        )
        val uriArr = ArrayList<String>()
        if (cursor != null) {
            while (cursor.moveToNext()) {
                // 사진 경로 Uri 가져오기
                val uri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                uriArr.add(uri)
            }
            cursor.close()
        }
        return uriArr
    }

    private fun showImagePopup(context: Context, uriArr: ArrayList<String>, position: Int, adapter: GridAdapter) {
        val imagePath = uriArr[position]
        val builder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.fragment_gallery_pick, null)
        builder.setView(dialogView)
        val imageView = dialogView.findViewById<ImageView>(R.id.galleryPickedImageView)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE

        Glide.with(context).load(imagePath).into(imageView)

        builder.setPositiveButton("Close") {dialog, _ -> dialog.dismiss()}

        builder.setNeutralButton("Remove") {_, _ ->

            val builderDelete = AlertDialog.Builder(context)
            builderDelete.setTitle("삭제하기")
            builderDelete.setMessage("정말로 해당 파일을 삭제하시겠습니까?")

            builderDelete.setPositiveButton("삭제") {_, _ ->
                if (deletePhoto(imagePath)) {       // 저장소에서 파일을 실제로 삭제한 후에
                    uriArr.removeAt(position)       // 사진들의 주소 목록인 uriArr에서도 삭제해서
                    adapter.notifyDataSetChanged()  // Adapter에도 삭제했음을 알린다. -> 그리드뷰에 곧바로 반영이 되어서 나타난다.
                    Toast.makeText(context, "삭제 성공!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "삭제 실패?", Toast.LENGTH_SHORT).show()
                }
            }
            builderDelete.setNegativeButton("취소") {dialog, _ -> dialog.dismiss()}
            builderDelete.create().show()
        }
        builder.create().show()
    }

    private fun deletePhoto(imagePath: String): Boolean {
        val resolver = requireContext().contentResolver
        val selection = MediaStore.Images.Media.DATA + "=?"
        val selectionArgs = arrayOf(imagePath)
        val deletedRows = resolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs)
        return deletedRows > 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}