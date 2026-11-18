import SwiftUI

struct PosterPuzzleTiles: View {
    let artwork: CGImage
    let board: PuzzleBoard
    let canMove: Bool
    let choose: (Int) -> Void
    let swap: (Int, Int) -> Void

    @Environment(\.colorScheme) private var scheme
    @GestureState private var drag: DragGesture.Value?
    private enum Area: Hashable { case board }

    private var dimension: Int { board.difficulty.rawValue }
    private var palette: EditorialPalette { EditorialPalette(scheme) }

    var body: some View {
        GeometryReader { geometry in
            let size = geometry.size
            let cell = CGSize(width: size.width / CGFloat(dimension), height: size.height / CGFloat(dimension))
            let source = drag.flatMap { position(at: $0.startLocation, cell: cell) }
            let target = drag.flatMap { position(at: $0.location, cell: cell) }
            ZStack(alignment: .topLeading) {
                VStack(spacing: 0) {
                    ForEach(0..<dimension, id: \.self) { row in
                        HStack(spacing: 0) {
                            ForEach(0..<dimension, id: \.self) { column in
                                let index = row * dimension + column
                                tile(index, cell: cell, dragged: index == source,
                                     targeted: index == target && index != source)
                            }
                        }
                    }
                }
                if let source, let drag {
                    art(tile: board.tiles[source], cell: cell)
                        .overlay { Rectangle().strokeBorder(palette.coral, lineWidth: 2) }
                        .shadow(radius: 6)
                        .position(
                            x: (CGFloat(source % dimension) + 0.5) * cell.width + drag.translation.width,
                            y: (CGFloat(source / dimension) + 0.5) * cell.height + drag.translation.height
                        )
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                }
            }
            .frame(width: size.width, height: size.height)
            .contentShape(Rectangle())
            .coordinateSpace(name: Area.board)
            .highPriorityGesture(dragGesture(cell: cell), including: canMove ? .all : .none)
        }
        .aspectRatio(2.0 / 3, contentMode: .fit)
    }

    private func tile(_ index: Int, cell: CGSize, dragged: Bool, targeted: Bool) -> some View {
        let piece = board.tiles[index]
        let selected = board.selected == index
        return Button {
            choose(index)
        } label: {
            art(tile: piece, cell: cell)
                .opacity(dragged ? 0.35 : 1)
                .overlay {
                    Rectangle()
                        .strokeBorder(selected || targeted ? palette.coral : palette.canvas,
                                      lineWidth: selected || targeted ? 3 : 0.75)
                        .allowsHitTesting(false)
                }
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!canMove)
        .accessibilityLabel("Piece \(piece + 1), row \(index / dimension + 1), column \(index % dimension + 1)")
        .accessibilityAddTraits(selected ? .isSelected : [])
        .accessibilityIdentifier("puzzle-tile-\(index)")
    }

    private func art(tile: Int, cell: CGSize) -> some View {
        Color.clear
            .frame(width: cell.width, height: cell.height)
            .overlay(alignment: .topLeading) {
                Image(decorative: artwork, scale: 1)
                    .resizable()
                    .frame(width: cell.width * CGFloat(dimension), height: cell.height * CGFloat(dimension))
                    .offset(x: -CGFloat(tile % dimension) * cell.width, y: -CGFloat(tile / dimension) * cell.height)
            }
            .clipped()
    }

    private func dragGesture(cell: CGSize) -> some Gesture {
        DragGesture(minimumDistance: 10, coordinateSpace: .named(Area.board))
            .updating($drag) { value, state, _ in
                if position(at: value.startLocation, cell: cell) != nil {
                    state = value
                }
            }
            .onEnded { value in
                guard let source = position(at: value.startLocation, cell: cell),
                      let target = position(at: value.location, cell: cell) else { return }
                swap(source, target)
            }
    }

    private func position(at point: CGPoint, cell: CGSize) -> Int? {
        guard cell.width > 0, cell.height > 0, point.x >= 0, point.y >= 0,
              point.x < cell.width * CGFloat(dimension),
              point.y < cell.height * CGFloat(dimension) else { return nil }
        return Int(point.y / cell.height) * dimension + Int(point.x / cell.width)
    }
}
