package com.example.lendmark.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lendmark.R
import com.example.lendmark.data.sources.announcement.AnnouncementItem
import com.example.lendmark.data.sources.announcement.AnnouncementType
import com.example.lendmark.databinding.*

class AnnouncementAdapter(
    private val items: List<AnnouncementItem>,
    private val onReviewClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is AnnouncementItem.ReviewEvent -> AnnouncementType.REVIEW_EVENT.type
        is AnnouncementItem.Weather -> AnnouncementType.WEATHER.type
        is AnnouncementItem.AcademicSchedule -> AnnouncementType.ACADEMIC.type
        is AnnouncementItem.PleaseGiveAPlus -> AnnouncementType.GIVE_A_PLUS.type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            AnnouncementType.REVIEW_EVENT.type ->
                ReviewEventViewHolder(ItemAnnouncementReviewBinding.inflate(inflater, parent, false))

            AnnouncementType.WEATHER.type ->
                WeatherViewHolder(ItemAnnouncementWeatherBinding.inflate(inflater, parent, false))

            AnnouncementType.ACADEMIC.type ->
                AcademicViewHolder(ItemAnnouncementAcademicBinding.inflate(inflater, parent, false))


            else ->
                APlusViewHolder(ItemAnnouncementAplusBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is ReviewEventViewHolder -> holder.bind(onReviewClick)
            is WeatherViewHolder -> holder.bind(item as AnnouncementItem.Weather)
            is AcademicViewHolder -> holder.bind(item as AnnouncementItem.AcademicSchedule)
            is APlusViewHolder -> holder.bind()
        }
    }

    override fun getItemCount() = items.size

    inner class WeatherViewHolder(val binding: ItemAnnouncementWeatherBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AnnouncementItem.Weather) {

            binding.tvTemp.text = item.temperature
            binding.tvDesc.text = item.description

            binding.ivWeatherIcon.setImageResource(item.iconRes)
        }
    }




    inner class AcademicViewHolder(val binding: ItemAnnouncementAcademicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AnnouncementItem.AcademicSchedule) {
            binding.tvDate.text = item.date
            binding.tvTitle.text = item.title
        }
    }

    inner class ReviewEventViewHolder(val binding: ItemAnnouncementReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(onClick: () -> Unit) {
            binding.btnReview.setOnClickListener { onClick() }
            binding.ivGift.setImageResource(R.drawable.gift)
        }
    }

    inner class APlusViewHolder(val binding: ItemAnnouncementAplusBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            // 정적 텍스트라 특별한 바인딩 필요 없음
        }
    }
}
