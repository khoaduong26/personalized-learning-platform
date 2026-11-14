export const practiceTopics = [
  {
    id: 'numbers', symbol: '123', title: 'Những con số',
    description: 'Đếm, so sánh và tìm số còn thiếu.', color: 'peach',
    questions: [
      { prompt: 'Số nào còn thiếu trong dãy số?', expression: '21, 22, …, 24', options: [20, 23, 25], answer: 23, hint: 'Mỗi số lớn hơn số đứng trước 1 đơn vị.', explanation: 'Sau 22 là 23. Ta có dãy số 21, 22, 23, 24.' },
      { prompt: 'Số nào lớn nhất?', expression: '36, 63, 53', options: [36, 63, 53], answer: 63, hint: 'Hãy so sánh chữ số hàng chục trước nhé.', explanation: '63 có 6 chục, lớn hơn 5 chục và 3 chục.' },
    ],
  },
  {
    id: 'operations', symbol: '+ −', title: 'Cộng và trừ',
    description: 'Cùng tính nhẩm, từng bước thật vui.', color: 'lavender',
    questions: [
      { prompt: 'Cùng tìm kết quả phép tính nhé!', expression: '24 + 15 = ?', options: [29, 39, 49], answer: 39, hint: 'Cộng hàng đơn vị: 4 + 5. Sau đó cộng hàng chục: 2 + 1.', explanation: '4 + 5 = 9 và 2 chục + 1 chục = 3 chục. Kết quả là 39!' },
      { prompt: 'Bớt đi một chút, còn bao nhiêu?', expression: '38 − 12 = ?', options: [26, 24, 36], answer: 26, hint: 'Trừ hàng đơn vị trước, rồi đến hàng chục.', explanation: '8 − 2 = 6 và 3 chục − 1 chục = 2 chục. Kết quả là 26!' },
    ],
  },
  {
    id: 'shapes', symbol: '△ □', title: 'Hình học quanh em',
    description: 'Khám phá hình dạng thật gần gũi.', color: 'mint',
    questions: [
      { prompt: 'Một hình tam giác có mấy cạnh?', expression: '△', options: [3, 4, 5], answer: 3, hint: 'Em thử đếm lần lượt các cạnh của hình tam giác nhé.', explanation: 'Hình tam giác có 3 cạnh và 3 đỉnh.' },
      { prompt: 'Một hình vuông có mấy cạnh?', expression: '□', options: [3, 4, 6], answer: 4, hint: 'Đếm cạnh trên, cạnh dưới và hai cạnh bên nhé.', explanation: 'Hình vuông có 4 cạnh bằng nhau.' },
    ],
  },
]
