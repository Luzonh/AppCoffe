package com.henryuide.pruebacoffe.view.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.henryuide.pruebacoffe.databinding.BoardItemBinding

class ViewPagerAdapter(
    private val boardList: List<Board>,
    private val onItemSelected: OnItemSelected? = null
) : RecyclerView.Adapter<ViewPagerAdapter.BoardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        return BoardViewHolder(
            BoardItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), onItemSelected
        )
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        holder.bind(boardList[position])
    }

    override fun getItemCount() = boardList.size

    inner class BoardViewHolder(
        val binding: BoardItemBinding,
        private val onItemSelected: OnItemSelected? = null
    ) :
        RecyclerView.ViewHolder(binding.root) {
        private val container = binding.container
        private val image = binding.animationView
        private val title = binding.tvTitle
        private val description = binding.tvDescriptions
        private val button = binding.btnNext

        fun bind(board: Board) = with(binding.root) {
            container.background = ContextCompat.getDrawable(context, board.background)
            image.setImageResource(board.image)
            //image.setAnimation(board.image)
            title.text = board.title
            description.text = board.description

            if (adapterPosition == (boardList.size - 1)) {
                button.text = "Finalizar"
            }

            button.setOnClickListener {
                onItemSelected?.onClickListener(adapterPosition)
            }
        }
    }

    interface OnItemSelected {
        fun onClickListener(position: Int)
    }
}