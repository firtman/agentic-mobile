enum PuzzleDifficulty: Int, CaseIterable, Identifiable {
    case three = 3, four = 4, six = 6

    var id: Self { self }
    var tileCount: Int { rawValue * rawValue }
}

struct PuzzleBoard {
    let difficulty: PuzzleDifficulty
    private(set) var tiles: [Int]
    private(set) var selected: Int?
    private(set) var moves = 0

    var correct: Int { tiles.indices.count { tiles[$0] == $0 } }
    var complete: Bool { correct == tiles.count }

    init(difficulty: PuzzleDifficulty, tiles: [Int]? = nil) {
        self.difficulty = difficulty
        let solved = Array(0..<difficulty.tileCount)
        if let tiles {
            precondition(tiles.count == solved.count && Set(tiles) == Set(solved))
            self.tiles = tiles
        } else {
            self.tiles = solved.shuffled()
            if self.tiles == solved { self.tiles.swapAt(0, 1) }
        }
    }

    mutating func choose(_ position: Int) {
        guard tiles.indices.contains(position), !complete else { return }
        if let selected {
            if selected == position {
                clearSelection()
            } else {
                swap(selected, with: position)
            }
        } else {
            selected = position
        }
    }

    mutating func swap(_ source: Int, with destination: Int) {
        guard !complete, source != destination,
              tiles.indices.contains(source), tiles.indices.contains(destination) else { return }
        tiles.swapAt(source, destination)
        selected = nil
        moves += 1
    }

    mutating func clearSelection() {
        selected = nil
    }
}
