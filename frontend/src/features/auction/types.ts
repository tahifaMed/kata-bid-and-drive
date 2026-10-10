export interface PhotoCredit {
  author: string
  license: string
  url: string
}

export interface AuctionListing {
  id: string
  title: string
  specs: string
  location: string
  timeLeft: string
  currentBid: number
  noReserve: boolean
  image: string
  imageAlt: string
  credit: PhotoCredit
}
